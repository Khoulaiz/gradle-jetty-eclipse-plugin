package com.sahlbach.gradle.plugins.jettyEclipse
import org.eclipse.jetty.security.LoginService
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.RequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.xml.XmlConfiguration
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Task
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.War
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory
import org.gradle.tooling.GradleConnector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * <p>Deploys a WAR to an embedded Jetty web container.</p>
 *
 * <p> Once started, the web container can be configured to run continuously,
 * rebuilding periodically the war file and automatically performing a hot redeploy when necessary. </p>
 */
class JettyEclipseStart extends ConventionTask {
    private static Logger logger = LoggerFactory.getLogger(JettyEclipseStart);

    static final String RELOAD_AUTOMATIC = "automatic";
    static final String RELOAD_MANUAL = "manual";

    private JettyEclipsePluginServer server;

    /**
     * List of connectors to use. If none are configured then we use a single SelectChannelConnector at port 8080
     */
    Connector[] connectors;

    int httpPort = 8080;

    /**
     * The context path for the webapp.
     */
    String contextPath = '/';

    /**
     * Port to listen to stop jetty on.
     */
    int stopPort;

    /**
     * Key to provide when stopping jetty.
     */
    String stopKey;

    /**
     * List of login services to set up. Optional.
     */
    LoginService[] loginServices;

    /**
     * A webdefault.xml file to use instead of the default for the webapp. Optional.
     */
    File webDefaultXml;

    /**
     * A web.xml file to be applied AFTER the webapp's web.xml file. Useful for applying different build profiles, eg test, production etc. Optional.
     */
    File overrideWebXml;

    /**
     * <p> Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests.
     * This is useful when starting the server with the intent to work with it interactively. </p><p> Often, it is desirable to let the server start and continue running subsequent processes in an
     * automated build environment. This can be facilitated by setting daemon to true. </p>
     */
    boolean daemon = false;

    /**
     * should the plugin try gradle rebuilds after x seconds to detect changes? 0 == disabled
     */
    int rebuildIntervalSeconds = 0;

    /**
     * Location of a jetty XML configuration file whose contents will be applied before any plugin configuration. Optional.
     */
    private File jettyConfig;

    /**
     * The "virtual" webapp created by the plugin.
     */
    private JettyEclipsePluginWebAppContext webAppConfig;

    String reload = RELOAD_AUTOMATIC;

    /**
     * A scanner to check ENTER hits on the console.
     */
    private Thread consoleScanner;

    /**
     * The war task we need to watch and get the output war
     */
    private War warTask;

    @Optional
    @InputFile
    File jettyConfig;

    @Optional
    @InputFiles
    private Iterable<File> additionalRuntimeJars = new ArrayList<File>();

    /**
     * A RequestLog implementation to use for the webapp at runtime. Optional.
     */
    private RequestLog requestLog;

    /**
     * Connector used for rebuilds
     */
    GradleConnector gradle;

    @TaskAction
    protected void start() {
        ClassLoader originalClassloader = Server.classLoader;
        List<File> additionalClasspath = new ArrayList<File>();
        for (File additionalRuntimeJar : additionalRuntimeJars) {
            additionalClasspath.add(additionalRuntimeJar);
        }
        URLClassLoader jettyClassloader = new URLClassLoader(
                new DefaultClassPath(additionalClasspath).asURLArray, originalClassloader);
        try {
            Thread.currentThread().contextClassLoader = jettyClassloader;
            startJetty();
        } finally {
            Thread.currentThread().contextClassLoader = originalClassloader;
        }
    }

    void startJetty() {
        validateConfiguration();
        ProgressLoggerFactory progressLoggerFactory = services.get(ProgressLoggerFactory);
        ProgressLogger progressLogger = progressLoggerFactory.newOperation(JettyEclipseStart);
        progressLogger.description = "Start Jetty server";
        progressLogger.shortDescription = "Starting Jetty";
        progressLogger.started();
        try {
            server = new JettyEclipsePluginServer();
            applyJettyXml();

            // setup connectors to use
            server.connectors = connectors;
            if (server.connectors == null || server.connectors.length == 0) {
                connectors = [server.createDefaultConnector(httpPort)];
                server.connectors = connectors;
            }

            // set up a RequestLog if one is provided
            if (requestLog != null) {
                server.requestLog = requestLog;
            }

            // set up the webapp and any context provided
            server.configureHandlers();
            configureWebApplication();
            server.addWebApplication(webAppConfig);

            // set up login services
            loginServices.each {
                logger.debug("${it.class.name} : $it");
            }
            server.loginServices = loginServices;

            // start Jetty
            server.start();

            if (daemon) {
                return;
            }

            if (stopPort != null && stopPort > 0 && stopKey != null) {
                Monitor monitor = new Monitor(stopPort, stopKey, (Server) server.proxiedObject);
                monitor.start();
            }

            // start the rebuild thread (if necessary)
            startRebuildThread();

            // start the new line scanner thread if necessary
            startConsoleScanner();

        } catch (Exception e) {
            throw new GradleException("Could not start the Jetty server.", e);
        } finally {
            progressLogger.completed();
        }

        progressLogger = progressLoggerFactory.newOperation(JettyEclipseStart);
        progressLogger.description = "Run Jetty at http://localhost:${httpPort}${webAppConfig.contextPath}";
        progressLogger.shortDescription = "Running at http://localhost:${httpPort}${webAppConfig.contextPath}";
        progressLogger.started();
        try {
            // keep the thread going if not in daemon mode
            server.join();
        } catch (Exception e) {
            throw new GradleException("Failed to wait for the Jetty server to stop.", e);
        } finally {
            progressLogger.completed();
        }
    }

    void startRebuildThread () {
        if(rebuildIntervalSeconds > 0) {
            gradle = GradleConnector.newConnector();
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new RebuildTask(), rebuildIntervalSeconds * 1000L, rebuildIntervalSeconds * 1000L);
        }
    }

    void configureWebApplication () {
        webAppConfig = new JettyEclipsePluginWebAppContext();
        webAppConfig.contextPath = contextPath?.startsWith("/") ? contextPath : "/" + contextPath;
        if (temporaryDir != null) {
            webAppConfig.tempDirectory = temporaryDir;
        }
        if (webDefaultXml != null) {
            webAppConfig.defaultsDescriptor = webDefaultXml.canonicalPath;
        }
        if (overrideWebXml != null) {
            webAppConfig.overrideDescriptor = overrideWebXml.canonicalPath;
        }

        // Don't treat JCL or Log4j as system classes
        Set<String> systemClasses = new LinkedHashSet<String>(Arrays.asList(webAppConfig.systemClasses));
        systemClasses.remove("org.apache.commons.logging.");
        systemClasses.remove("org.apache.log4j.");
        webAppConfig.systemClasses = systemClasses.toArray(new String[systemClasses.size()]);
        webAppConfig.parentLoaderPriority = false;
        webAppConfig.war = warTask.archivePath;

        logger.info("War file = " + webAppConfig.war);
        logger.info("Context path = " + webAppConfig.contextPath);
        logger.info("Tmp directory = " + " determined at runtime");
        logger.info("Web defaults = " + (webAppConfig.defaultsDescriptor == null ? " jetty default"
                                         : webAppConfig.defaultsDescriptor));
        logger.info("Web overrides = " + (webAppConfig.overrideDescriptor == null ? " none"
                                          : webAppConfig.overrideDescriptor));
    }

    /**
     * We need to be depending on a war task
     */
    void validateConfiguration () {
        warTask = null;
        def warTasks = project.tasks.withType(War);
        for (dep in dependsOn) {
            if(dep instanceof String) {
                Task task = project.getTasksByName(dep,false).iterator().next();
                if(warTasks.contains(task)) {
                    warTask = task as War;
                }
            }
        }
        logger.info("warTask: $warTask");
        if(warTask == null) {
            throw new InvalidUserDataException("This task will work on the war output of a war task. Please make the" +
                                               " task $name dependent on a war task");
        }
    }

    /**
     * Run a thread that monitors the console input to detect ENTER hits.
     */
    private void startConsoleScanner() {
        if (RELOAD_MANUAL.equalsIgnoreCase(reload)) {
            logger.info("Console reloading is ENABLED. Hit ENTER on the console to restart the context.");
            consoleScanner = new ConsoleScanner(this);
            consoleScanner.start();
        }
    }

    public void restartWebApp(boolean reconfigureScanner) throws Exception {
        logger.info("Restarting webapp ...");
        logger.debug("Stopping webapp ...");
        webAppConfig.stop();
        logger.debug("Reconfiguring webapp ...");
        validateConfiguration();
        logger.debug("Restarting webapp ...");
        webAppConfig.start();
        logger.info("Restart completed.");
    }

    void applyJettyXml() throws Exception {
        if (jettyConfig == null) {
            return;
        }
        logger.info("Configuring Jetty from xml configuration file = {}", jettyConfig);
        XmlConfiguration xmlConfiguration = new XmlConfiguration(jettyConfig.toURI().toURL());
        xmlConfiguration.configure(server.proxiedObject);
    }

    /**
     * This task schedules a rebuild and checks, if the task was actually executed (not skipped etc.)
     * if the task was executed, the webapp is restarted
     */
    private class RebuildTask extends TimerTask {
        @Override
        public void run() {
            long startTS = System.currentTimeMillis();
            logger.info('state before:' + warTask.state.didWork);
            gradle.connect().newBuild().forTasks(warTask.path).run();
            logger.info('state after:' + warTask.state.didWork);
            logger.info('time: '+System.currentTimeMillis() - startTS);
        }
    }
}
