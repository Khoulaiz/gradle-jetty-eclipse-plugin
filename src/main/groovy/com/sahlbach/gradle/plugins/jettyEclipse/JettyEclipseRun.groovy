/*
 * Copyright 2012-2013 the original author or authors.
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

import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.security.SecurityHandler
import org.eclipse.jetty.security.authentication.BasicAuthenticator
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.NCSARequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.xml.XmlConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Task
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.War
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
/**
 * <p>Deploys a WAR to an embedded Jetty web container.</p>
 *
 * <p> Once started, the web container can be configured to run continuously,
 * rebuilding periodically the war file and automatically performing a hot redeploy when necessary. </p>
 */
class JettyEclipseRun extends DefaultTask {

    private JettyEclipsePluginServer server
    private Connector[] connectors

    /**
     * the httpPort jetty will listen to
     */
    @Optional
    @Input
    Integer httpPort

    /**
     * The context path for the webapp.
     */
    @Optional
    @Input
    String contextPath

    /**
     * Port to listen to stop jetty on.
     */
    @Optional
    @Input
    Integer stopPort

    /**
     * Key to provide when stopping jetty.
     */
    @Optional
    @Input
    String stopKey

    /**
     * User File to use for a convenient way to setup a security service
     * A file containing lines with the following content "user:pwd[,role]" eg. "ace: joshua,admin"
     * Will be used with a BasicAuthenticator for the server.
     */
    @Optional
    @InputFile
    File passwordFile

    /**
     * A webdefault.xml file to use instead of the default for the webapp. Optional.
     */
    @Optional
    @Input
    File webDefaultXml

    /**
     * A web.xml file to be applied AFTER the webapp's web.xml file. Useful for applying different build profiles, eg test, production etc. Optional.
     */
    @Optional
    @Input
    File overrideWebXml

    /**
     * <p> Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests.
     * This is useful when starting the server with the intent to work with it interactively. </p><p> Often, it is desirable to let the server start and continue running subsequent processes in an
     * automated build environment. This can be facilitated by setting daemon to true. </p>
     */
    @Optional
    @Input
    Boolean daemon

    /**
     * should the plugin try gradle rebuilds after x seconds to detect changes? 0 == disabled
     */
    @Optional
    @Input
    Integer rebuildIntervalInSeconds

    /**
     * Location of a jetty XML configuration file whose contents will be applied before any plugin configuration. Optional.
     */
    @Optional
    @InputFile
    File jettyConfig

    /**
     * true: reload webapp automatically as soon as changes are detected
     */
    @Optional
    @Input
    Boolean automaticReload;

    /**
     * if > 0 the interval in seconds we check the war file for changes
     */
    @Optional
    @Input
    Integer scanIntervalInSeconds;

    @Optional
    @InputFiles
    Iterable<File> additionalRuntimeJars = new ArrayList<File>()

    /**
     * The war file we use for the web app. We will watch this file but copy it for the server
     */
    @Optional
    @InputFile
    File warFile

    /**
     * The task to build for the automatic rebuild
     */
    @Optional
    @Input
    Task rebuildTask

    /**
     * A RequestLog implementation to use for the webapp at runtime. Optional.
     */
    @Optional
    @OutputFile
    File requestLog

    /**
     * The "virtual" webapp created by the plugin.
     */
    JettyEclipsePluginWebAppContext webAppContext

    /**
     * the scheduled future for scheduling rebuilds
     */
    ScheduledFuture<?> rebuildTimerFuture

    /**
     * the scheduled future for watching the war file
     */
    ScheduledFuture<?> fileWatchFuture

    /**
     * the scheduled future for watching user input
     */
    ScheduledFuture<?> consoleScannerFuture

    /**
     * connector used for background builds
     */
    GradleConnector gradleConnector

    /**
     * synchronization object for multithreaded access
     */
    final Object semaphore = new Object()

    /**
     * executor service for the multithreading repeating tasks
     */
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * lastModified of the war File
     */
    long warFileLastModified

    /**
     * prevent manual reload if true
     */
    boolean manualReloadDisabled = false

    /**
     * Rebuild closure for background builds
     */
    def rebuildClosure = {
        logger.debug('Attempting to rebuild')
        ProjectConnection connection = gradleConnector.connect()
        synchronized(semaphore) {
            logger.warn('Rebuilding webapp...')
            try {
                ByteArrayOutputStream stdout = new ByteArrayOutputStream()
                ByteArrayOutputStream stderr = new ByteArrayOutputStream()
                BuildLauncher launcher = connection.newBuild().forTasks(rebuildTask.path)
                launcher.standardOutput = stdout
                launcher.standardError = stderr
                launcher.run()
                logger.debug('Rebuild finished')
            } catch (Exception e) {
                logger.debug("Build ended with exception: "+e)
            } finally {
                connection.close()
            }
        }
    }

    /**
     * file watcher closure to monitor changes of the original war file
     */
    def fileWatchClosure = {
        boolean changed = false
        synchronized(semaphore) {
            try {
                if(!warFile.exists() || !warFile.canRead()) {
                    if(!manualReloadDisabled) {
                        manualReloadDisabled = true
                        logger.warn("---> War file isn't readable. Reloading webapp is temporary disabled.")
                    }
                    warFileLastModified = 0
                } else {
                    if(manualReloadDisabled) {
                        logger.warn("---> War file is readable. Reloading webapp enabled.")
                        manualReloadDisabled = false
                    }
                    if(warFileLastModified != warFile.lastModified()) {
                        changed = true
                        warFileLastModified = warFile.lastModified()
                    }
                }
            } catch (Exception e) {
                warFileLastModified = 0
            }
        }
        if(changed) {
            if(automaticReload) {
                logger.warn("---> File watcher detected changes of the war file. Reloading webapp automatically as configured.")
                reloadWebApp()
            } else {
                logger.warn("---> File watcher detected changes of the war file. Press ENTER to reload webapp.")
            }
        }
    }

    /**
     * Console scanner to watch for user input
     */
    def consoleScannerClosure = {
        while (System.in.available() > 0) {
            int inputByte = System.in.read()
            if (inputByte >= 0) {
                char c = (char) inputByte
                if (c == '\n' && !manualReloadDisabled) {
                    reloadWebApp()
                    clearInputBuffer()
                } else if((c == 'r') || (c == 'R')) {
                    int secondByte = System.in.read()
                    if(secondByte >= 0) {
                        char c2 = (char) secondByte
                        if(c2 == '\n') {
                            if(rebuildTask != null) {
                                if(rebuildTimerFuture != null) {
                                    rebuildTimerFuture.cancel(true)
                                    rebuildTimerFuture = null
                                }
                                rebuildClosure.call()
                                if(c == 'r' && !manualReloadDisabled)
                                    reloadWebApp()
                                startRebuildThread()
                                clearInputBuffer()
                            } else {
                                logger.warn('No rebuild task available. Define a rebuild task or make this task dependent to a war task.')
                            }
                        }
                    }
                }
            }
        }
    }

    @TaskAction
    protected void start() {
        initFromExtension()
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
            def conList = []
            conList += server.createDefaultHttpConnector(httpPort)
            server.connectors = conList

            // set up a RequestLog if one is provided
            if (requestLog != null) {
                requestLog.parentFile.mkdirs()
                server.requestLog = new NCSARequestLog(requestLog.canonicalPath)
            }

            // set up the webapp and any context provided
            server.configureHandlers()
            configureWebApplication()
            server.addWebApplication(webAppContext)

            // start Jetty
            server.start()

            if (daemon) {
                return
            }

            // start the rebuild thread (if necessary)
            startRebuildThread()

            // start the file watcher thread (if necessary)
            startWatcherThread()

            // start the new line scanner
            startConsoleScanner()

            if (stopPort > 0 && stopKey != null) {
                Monitor monitor = new Monitor(stopPort, stopKey, (Server) server.proxiedObject)
                monitor.start()
            }

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
             rebuildTimerFuture = scheduler.scheduleWithFixedDelay(rebuildClosure,
                                                                   rebuildIntervalInSeconds,
                                                                   rebuildIntervalInSeconds,
                                                                   TimeUnit.SECONDS)
        }
    }

    void startWatcherThread () {
        if(scanIntervalInSeconds > 0) {
            fileWatchFuture = scheduler.scheduleWithFixedDelay(fileWatchClosure,
                                                               scanIntervalInSeconds,
                                                               scanIntervalInSeconds,
                                                               TimeUnit.SECONDS)
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

        setupWar(webAppContext, warFile)
        if(passwordFile != null) {
            if (passwordFile.canRead()) {
                SecurityHandler securityHandler = new ConstraintSecurityHandler()
                securityHandler.loginService = new HashLoginService("Gradle",passwordFile.canonicalPath)
                securityHandler.authenticator = new BasicAuthenticator()
                webAppContext.securityHandler = securityHandler
            } else {
                logger.error("Passwordfile $passwordFile.canonicalPath not readable")
            }
        }
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
     * @param webAppContext webapp to configure
     * @param warFile file to use as webApp
     * @return destination file
     */
    private void setupWar (JettyEclipsePluginWebAppContext webAppContext, File warFile) {
        synchronized (semaphore) {
            File destination = new File(project.buildDir, "tmp/${JettyEclipsePlugin.JETTY_ECLIPSE_PLUGIN}/war/")
            destination.deleteDir()
            destination.mkdirs()
            destination = new File(destination, "${warFile.name}")
            destination.bytes = warFile.bytes
            webAppContext.war = destination.canonicalPath
            warFileLastModified = warFile.lastModified()
        }
    }

    /**
     * check for war file
     * 1. check for war file path given by user
     * 2. if not given by user, check for war task dependency and use the output war
     * check for rebuilding request.
     * 1. check for task given
     * 2. if not given by user, check for war task dependency and use the task for rebuild
     */
    void validateConfiguration () {
        if(warFile == null) {
            // user gave no warfile, try to find one via dependencies
            def warTasks = project.tasks.withType(War)
            for (dep in dependsOn) {
                if(dep instanceof String) {
                    Task task = project.getTasksByName(dep,false).iterator().next()
                    if(warTasks.contains(task)) {
                        War warTask = task as War
                        warFile = warTask.archivePath
                        if (rebuildTask == null) {
                            rebuildTask = warTask
                        }
                        break
                    }
                }
            }
        }
        if (warFile == null) {
            throw new InvalidUserDataException("Please specify a warFile to use or make this task dependent " +
                                               "of a war task which output will be used as war file")
        }
        if(!warFile.exists() || !warFile.canRead()) {
            throw new InvalidUserDataException("The specified war file $warFile.canonicalPath cannot be read.")
        }
        gradleConnector = GradleConnector.newConnector().forProjectDirectory(project.rootDir)
        logger.debug("warFile used: $warFile.canonicalPath")
        logger.debug("rebuildTask used: $rebuildTask")
    }

    /**
     * Run a thread that monitors the console input to detect user input.
     */
    private void startConsoleScanner() {
        String output = '\nHit <ENTER> to reload the webapp.\n'
        if(rebuildTask != null) {
            output += 'Hit r + <ENTER> to rebuild and reload the webapp.\n'
            output += 'Hit R + <ENTER> to rebuild the webapp without reload'
            if(automaticReload)
                output += ' (may trigger automatic reload).'
            output += '\n'
        }
        logger.warn(output)
        consoleScannerFuture = scheduler.scheduleWithFixedDelay(consoleScannerClosure,1,1,TimeUnit.SECONDS)
    }

    public void reloadWebApp () throws Exception {
        synchronized(semaphore) {
            logger.warn("Reloading webapp ...")
            try {
                if(rebuildTimerFuture != null) {
                    rebuildTimerFuture.cancel(true)
                    rebuildTimerFuture = null
                }
                logger.info("Stopping webapp ...")
                webAppContext.stop()
                logger.info("Reconfiguring webapp ...")
                validateConfiguration()
                setupWar(webAppContext,warFile)
                logger.info("Restarting webapp ...")
                webAppContext.start()
                logger.info("Restart completed.")
            } finally {
                startRebuildThread()
            }
        }
    }

    void applyJettyXml() throws Exception {
        if (jettyConfig == null) {
            return
        }
        logger.info("Configuring Jetty from xml configuration file = {}", jettyConfig)
        XmlConfiguration xmlConfiguration = new XmlConfiguration(jettyConfig.toURI().toURL())
        xmlConfiguration.configure(server.proxiedObject)
    }

    /**
     * Skip buffered bytes of system console.
     */
    private static void clearInputBuffer() {
        try {
            while (System.in.available() > 0) {
                // System.in.skip doesn't work properly. I don't know why
                long available = System.in.available()
                for (int i = 0; i < available; i++) {
                    if (System.in.read() == -1) {
                        break
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    void initFromExtension() {
        JettyEclipsePluginExtension extension = project.extensions
                                                       .getByName(JettyEclipsePlugin.JETTY_ECLIPSE_EXTENSION) as JettyEclipsePluginExtension
        if(httpPort == null)
            httpPort = extension.httpPort

        if(stopPort == null)
            stopPort = extension.stopPort

        if(stopKey == null)
            stopKey = extension.stopKey

        if(warFile == null)
            warFile = extension.warFile

        if(scanIntervalInSeconds == null)
            scanIntervalInSeconds = extension.scanIntervalInSeconds

        if(contextPath == null)
            contextPath = extension.contextPath

        if(webDefaultXml == null)
            webDefaultXml = extension.webDefaultXml

        if(overrideWebXml == null)
            overrideWebXml = extension.overrideWebXml

        if(jettyConfig == null)
            jettyConfig = extension.jettyConfig

        if(automaticReload == null)
            automaticReload = extension.automaticReload

        if(rebuildIntervalInSeconds == null)
            rebuildIntervalInSeconds = extension.rebuildIntervalInSeconds

        if(rebuildTask == null)
            rebuildTask = extension.rebuildTask

        if(requestLog == null)
            requestLog = extension.requestLog

        if(passwordFile == null)
            passwordFile = extension.passwordFile

        if(daemon == null)
            daemon = extension.daemon
    }
}
