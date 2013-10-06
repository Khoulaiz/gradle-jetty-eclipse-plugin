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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * A {@link Plugin} to start an embedded eclipse jetty server
 */
class JettyEclipsePlugin implements Plugin<Project> {

    private final static Logger logger = LoggerFactory.getLogger(JettyEclipsePlugin);

    static final String JETTY_ECLIPSE_RUN = "jettyEclipseRun"
    static final String JETTY_ECLIPSE_STOP = "jettyEclipseStop"
    static final String JETTY_ECLIPSE_PLUGIN = "jettyEclipse"
    static final String JETTY_ECLIPSE_EXTENSION = JETTY_ECLIPSE_PLUGIN

    protected Project project
    protected JettyEclipsePluginExtension extension

    @Override
    void apply (Project project) {
        this.project = project

        project.plugins.apply(WarPlugin.class)

        extension = createExtension()
        configureJettyStart()
        configureJettyStop()
    }

    protected JettyEclipsePluginExtension createExtension() {
        extension = project.extensions.create(JETTY_ECLIPSE_EXTENSION, JettyEclipsePluginExtension)
        extension.with {
            // Defaults for extension
            httpPort = 8080
            stopPort = 8090
            stopKey = "stop"
            contextPath = ''
            scanIntervalInSeconds = 5
            automaticReload = false
            rebuildIntervalInSeconds = 0
            daemon = false
            skipAnnotations = false
        }
        logger.info("Adding JettyEclipse extension");
        return extension
    }

    private void configureJettyStart () {
        JettyEclipseRun jettyStart = project.tasks.create(JETTY_ECLIPSE_RUN, JettyEclipseRun)
        jettyStart.description = "Deploys your war to an embedded jetty and allows easy rebuild and reload."
        jettyStart.group = WarPlugin.WEB_APP_GROUP
        jettyStart.dependsOn(WarPlugin.WAR_TASK_NAME)
    }

    private void configureJettyStop() {
        JettyEclipseStop jettyStop = project.tasks.create(JETTY_ECLIPSE_STOP, JettyEclipseStop)
        jettyStop.description = "Stops the embedded jetty server."
        jettyStop.group = WarPlugin.WEB_APP_GROUP
    }
}
