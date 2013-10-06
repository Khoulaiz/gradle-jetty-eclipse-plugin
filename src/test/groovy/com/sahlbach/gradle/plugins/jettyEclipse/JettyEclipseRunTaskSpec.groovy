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
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JettyEclipseRunTaskSpec extends Specification {

    def "Creates new JettyEclipseRun task and checks defaults"() {
        setup:
            Project project = ProjectBuilder.builder().build()
            project.apply plugin: JettyEclipsePlugin
            JettyEclipsePluginExtension ext = project.extensions.getByName(JettyEclipsePlugin.JETTY_ECLIPSE_EXTENSION) as JettyEclipsePluginExtension
        when:
            JettyEclipseRun task = project.tasks.create('customRun', JettyEclipseRun)
            ext.httpPort = 1
            ext.contextPath = "2"
            ext.stopPort = 3
            ext.stopKey = "4"
            ext.webDefaultXml = new File("5")
            ext.overrideWebXml = new File("6")
            ext.daemon = !ext.daemon
            ext.rebuildIntervalInSeconds = 7
            ext.jettyConfig = new File("8")
            ext.automaticReload = !ext.automaticReload
            ext.scanIntervalInSeconds = 9
            ext.warFile = new File("10")
            ext.rebuildTask = task
            ext.requestLog = new File("11")
            task.initFromExtension()
        then:
            task.dependsOn(WarPlugin.WAR_TASK_NAME)
            task.httpPort == ext.httpPort
            task.contextPath == ext.contextPath
            task.stopPort == ext.stopPort
            task.stopKey == ext.stopKey
            task.webDefaultXml == ext.webDefaultXml
            task.overrideWebXml == ext.overrideWebXml
            task.daemon == ext.daemon
            task.rebuildIntervalInSeconds == ext.rebuildIntervalInSeconds
            task.jettyConfig == ext.jettyConfig
            task.automaticReload == ext.automaticReload
            task.scanIntervalInSeconds == ext.scanIntervalInSeconds
            task.warFile == ext.warFile
            task.rebuildTask == ext.rebuildTask
            task.requestLog == ext.requestLog
    }
}
