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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin
/**
 * A {@link Plugin} to start an embedded eclipse jetty server
 */
class JettyEclipsePlugin implements Plugin<Project> {
    static final String JETTY_ECLIPSE_START = "jettyEclipseRun";
    static final String JETTY_ECLIPSE_STOP = "jettyEclipseStop";
    static final String JETTY_ECLIPSE_PLUGIN = "jettyEclipse";

    private Project project

    @Override
    void apply (Project project) {
        this.project = project
        project.plugins.apply(WarPlugin.class)
        configureJettyStart()
        configureJettyStop()
    }

    private void configureJettyStart () {
        JettyEclipseRun jettyStart = project.tasks.create(JETTY_ECLIPSE_START, JettyEclipseRun)
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
