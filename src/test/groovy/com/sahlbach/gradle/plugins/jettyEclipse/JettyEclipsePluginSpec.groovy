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

class JettyEclipsePluginSpec extends Specification {

    def "Applies plugin and checks plugin setup"() {
        setup:
            Project project = ProjectBuilder.builder().build()
        expect:
            !project.plugins.hasPlugin(WarPlugin)
        when:
            project.apply plugin: JettyEclipsePlugin
        then:
            project.plugins.hasPlugin(WarPlugin)
            project.extensions.getByName(JettyEclipsePlugin.JETTY_ECLIPSE_EXTENSION) instanceof JettyEclipsePluginExtension
    }

    def "Applies plugin and checks JettyEclipseRun task setup"() {
        setup:
            Project project = ProjectBuilder.builder().build()
        when:
            project.apply plugin: JettyEclipsePlugin
        then:
            def task = project.tasks[JettyEclipsePlugin.JETTY_ECLIPSE_RUN]
            task instanceof JettyEclipseRun
            task.dependsOn(WarPlugin.WAR_TASK_NAME)
    }

    def "Applies plugin and checks JettyEclpseStop task setup"() {
        setup:
            Project project = ProjectBuilder.builder().build()
        when:
            project.apply plugin: JettyEclipsePlugin
        then:
            def task = project.tasks[JettyEclipsePlugin.JETTY_ECLIPSE_STOP]
            task instanceof JettyEclipseStop
    }
}
