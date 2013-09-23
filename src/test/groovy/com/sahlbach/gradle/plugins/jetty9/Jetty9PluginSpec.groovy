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

package com.sahlbach.gradle.plugins.jetty9
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class Jetty9PluginSpec extends Specification {
    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
    }

    def "Applies plugin and checks plugin setup"() {
        expect:
            !project.plugins.hasPlugin(WarPlugin)
        when:
            project.apply plugin: Jetty9Plugin
        then:
            project.plugins.hasPlugin(WarPlugin)
            project.convention.plugins.jettyEclipse instanceof Jetty9PluginConvention
    }

    def "Applies plugin and checks JettyRun task setup"() {
        when:
            project.apply plugin: Jetty9Plugin
        then:
            def task = project.tasks[Jetty9Plugin.JETTY_RUN]
            task instanceof JettyRun
            task.httpPort == project.httpPort
            task.dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    }

    def "Applies plugin and checks JettyRunWar task setup"() {
        when:
            project.apply plugin: Jetty9Plugin
        then:
            def task = project.tasks[Jetty9Plugin.JETTY_RUN_WAR]
            task instanceof JettyRunWar
            task.httpPort == project.httpPort
            task.dependsOn(WarPlugin.WAR_TASK_NAME)
    }

    def "Applies plugin and checks JettyStop task setup"() {
        when:
            project.apply plugin: Jetty9Plugin
        then:
            def task = project.tasks[Jetty9Plugin.JETTY_STOP]
            task instanceof JettyStop
            task.stopPort == project.stopPort
    }
}
