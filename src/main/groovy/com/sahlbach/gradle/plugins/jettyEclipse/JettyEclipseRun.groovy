/*
 * Coyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.War
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * <p>Deploys a WAR to an embedded Jetty web container.</p>
 *
 * <p> Once started, the web container can be configured to run continuously,
 * rebuilding periodically the war file and automatically performing a hot redeploy when necessary. </p>
 */
class JettyEclipseRun extends ConventionTask implements BuildObserver {
    public static Logger logger = LoggerFactory.getLogger(JettyEclipseRun);

    private JettyEclipsePluginServer server

    /**
     * List of connectors to use. If none are configured then we use a single SelectChannelConnector at port 8080
     */
    Connector[] connectors

    int httpPort = 8080

    /**
     * The context path for the webapp.
     */
    String contextPath = ""

    /**
     * Port to listen to stop jetty on.
     */
    int stopPort

    /**
     * Key to provide when stopping jetty.
     */
    String stopKey

    /**
     * List of login services to set up. Optional.
     */
    LoginService[] loginServices

    /**
     * A webdefault.xml file to use instead of the default for the webapp. Optional.
     */
    File webDefaultXml

    /**
     * A web.xml file to be applied AFTER the webapp's web.xml file. Useful for applying different build profiles, eg test, production etc. Optional.
     */
    File overrideWebXml

    /**
     * <p> Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests.
     * This is useful when starting the server with the intent to work with it interactively. </p><p> Often, it is desirable to let the server start and continue running subsequent processes in an
     * automated build environment. This can be facilitated by setting daemon to true. </p>
     */
    boolean daemon = false

    /**
     * should the plugin try gradle rebuilds after x seconds to detect changes? 0 == disabled
     */
    int rebuildIntervalInSeconds = 0

    /**
     * Location of a jetty XML configuration file whose contents will be applied before any plugin configuration. Optional.
     */
    private File jettyConfig

    /**
     * The "virtual" webapp created by the plugin.
     */
    private JettyEclipsePluginWebAppContext webAppContext

    @Optional
    @Input
    boolean automaticReload = false;

    /**
     * A scanner to check ENTER hits on the console.
     */
    private Thread consoleScanner

    /**
     * The war task we need to watch and get the output war
     */
    private War warTask

    @Optional
    @InputFile
    File jettyConfig

    @Optional
    @InputFiles
    private Iterable<File> additionalRuntimeJars = new ArrayList<File>()

    /**
     * A RequestLog implementation to use for the webapp at runtime. Optional.
     */
    private RequestLog requestLog

    /**
     * the timer task for scheduling rebuilds
     */
    private RebuildTimerTask rebuildTimerTask

    @TaskAction
    protected void start() {
        ClassLoader originalClassloader = Server.classLoader
        List<File> additionalClasspath = new ArrayList<File>()
        for (File additionalRuntimeJar : additionalRuntimeJars) {
            additionalClasspath.add(additionalRuntimeJar)
        }
        URLClassLoader jettyClassloader = new URLClassLoader(
                new DefaultClassPath(additionalClasspath).asURLArray, originalClassloader)
        try {
            Thread.currentThread().contextClassLoader = jettyClassloader
            startJetty()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassloader
        }
    }

    void startJetty() {
        validateConfiguration()
        ProgressLoggerFactory progressLoggerFactory = services.get(ProgressLoggerFactory)
        ProgressLogger progressLogger = progressLoggerFactory.newOperation(JettyEclipseRun)
        progressLogger.description = "Start Jetty server"
        progressLogger.shortDescription = "Starting Jetty"
        progressLogger.started()
        try {
            server = new JettyEclipsePluginServer()
            applyJettyXml()

            // setup connectors to use
            server.connectors = connectors
            if (server.connectors == null || server.connectors.length == 0) {
                connectors = [server.createDefaultConnector(httpPort)]
                server.connectors = connectors
            }

            // set up a RequestLog if one is provided
            if (requestLog != null) {
                server.requestLog = requestLog
            }

            // set up the webapp and any context provided
            server.configureHandlers()
            configureWebApplication()
            server.addWebApplication(webAppContext)

            // set up login services
            loginServices.each {
                logger.debug("${it.class.name} : $it")
            }
            server.loginServices = loginServices

            // start Jetty
            server.start()

            if (daemon) {
                return
            }

            if (stopPort != null && stopPort > 0 && stopKey != null) {
                Monitor monitor = new Monitor(stopPort, stopKey, (Server) server.proxiedObject)
                monitor.start()
            }

            // start the rebuild thread (if necessary)
            startRebuildThread()

            // start the new line scanner thread if necessary
            startConsoleScanner()

        } catch (Exception e) {
            throw new GradleException("Could not start the Jetty server.", e)
        } finally {
            progressLogger.completed()
        }

        progressLogger = progressLoggerFactory.newOperation(JettyEclipseRun)
        progressLogger.description = "Run Jetty at http://localhost:${httpPort}${webAppContext.contextPath}"
        progressLogger.shortDescription = "Running at http://localhost:${httpPort}${webAppContext.contextPath}"
        progressLogger.started()
        try {
            // keep the thread going if not in daemon mode
            server.join()
        } catch (Exception e) {
            throw new GradleException("Failed to wait for the Jetty server to stop.", e)
        } finally {
            progressLogger.completed()
        }
    }

    void startRebuildThread () {
        if(rebuildIntervalInSeconds > 0) {
            rebuildTimerTask = new RebuildTimerTask(this, warTask,rebuildIntervalInSeconds)
        }
    }

    void configureWebApplication () {
        webAppContext = new JettyEclipsePluginWebAppContext()
        webAppContext.contextPath = contextPath.empty || contextPath.startsWith("/") ? contextPath : "/" + contextPath
        if (temporaryDir != null) {
            webAppContext.tempDirectory = temporaryDir
        }
        if (webDefaultXml != null) {
            webAppContext.defaultsDescriptor = webDefaultXml.canonicalPath
        }
        if (overrideWebXml != null) {
            webAppContext.overrideDescriptor = overrideWebXml.canonicalPath
        }

        // Don't treat JCL or Log4j as system classes
        Set<String> systemClasses = new LinkedHashSet<String>(Arrays.asList(webAppContext.systemClasses))
        systemClasses.remove("org.apache.commons.logging.")
        systemClasses.remove("org.apache.log4j.")
        webAppContext.systemClasses = systemClasses.toArray(new String[systemClasses.size()])
        webAppContext.parentLoaderPriority = false

        setupWarFromTask(webAppContext, warTask)

        logger.info("War file = " + webAppContext.war)
        logger.info("Context path = " + webAppContext.contextPath)
        logger.info("Tmp directory = " + " determined at runtime")
        logger.info("Web defaults = " + (webAppContext.defaultsDescriptor == null ? " jetty default"
                                         : webAppContext.defaultsDescriptor))
        logger.info("Web overrides = " + (webAppContext.overrideDescriptor == null ? " none"
                                          : webAppContext.overrideDescriptor))
    }

    /**
     * Copy the war file to a temp location and use it as webapp war
     * @param warTask to grab war from
     * @return destination file
     */
    private void setupWarFromTask (JettyEclipsePluginWebAppContext webAppContext, War warTask) {
        File destination = new File(project.buildDir, "tmp/${JettyEclipsePlugin.JETTY_ECLIPSE_PLUGIN}/war/")
        destination.deleteDir()
        destination.mkdirs()
        destination = new File(destination, "${warTask.archiveName}")
        destination.bytes = warTask.archivePath.bytes
        webAppContext.war = destination.canonicalPath
    }

    /**
     * We need to be depending on a war task
     */
    void validateConfiguration () {
        warTask = null
        def warTasks = project.tasks.withType(War)
        for (dep in dependsOn) {
            if(dep instanceof String) {
                Task task = project.getTasksByName(dep,false).iterator().next()
                if(warTasks.contains(task)) {
                    warTask = task as War
                }
            }
        }
        logger.debug("warTask used: $warTask")
        if(warTask == null) {
            throw new InvalidUserDataException("This task will work on the war output of a war task. Please make the" +
                                               " task $name dependent on a war task")
        }
    }

    /**
     * Run a thread that monitors the console input to detect ENTER hits.
     */
    private void startConsoleScanner() {
        if (!automaticReload) {
            logger.warn("Console reloading is ENABLED. Hit ENTER on the console to reload the webapp.")
            consoleScanner = new ConsoleScanner(this)
            consoleScanner.start()
        }
    }

    public void reloadingWebApp () throws Exception {
        logger.warn("Reloading webapp ...")
        if(rebuildTimerTask != null) {
            rebuildTimerTask.stop()
            rebuildTimerTask = null
        }
        logger.info("Stopping webapp ...")
        webAppContext.stop()
        logger.info("Reconfiguring webapp ...")
        validateConfiguration()
        setupWarFromTask(webAppContext,warTask)
        logger.info("Restarting webapp ...")
        webAppContext.start()
        logger.info("Restart completed.")
    }

    void applyJettyXml() throws Exception {
        if (jettyConfig == null) {
            return
        }
        logger.info("Configuring Jetty from xml configuration file = {}", jettyConfig)
        XmlConfiguration xmlConfiguration = new XmlConfiguration(jettyConfig.toURI().toURL())
        xmlConfiguration.configure(server.proxiedObject)
    }

    @Override
    void notifyBuildFailure () {
        logger.info("Background build failt.")
    }

    @Override
    void notifyBuildWithNewWar () {
        rebuildTimerTask.stop()
        if(automaticReload) {
            logger.warn("---> Background rebuild detected changes. Reloading webapp automatically as configured.")
            reloadingWebApp()
        } else {
            logger.warn("---> Background rebuild detected changes. Press ENTER to reload webapp.")
        }
    }

    @Override
    void notifyBuildWithoutChanges () {
        logger.info("Background rebuild detected no changes.")
    }
}
