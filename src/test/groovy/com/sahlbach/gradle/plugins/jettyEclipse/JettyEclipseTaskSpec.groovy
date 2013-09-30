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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JettyEclipseTaskSpec extends Specification {
    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: JettyEclipsePlugin
    }

    def "Creates new custom JettyRun task"() {
        when:
            project.tasks.create('customRun', JettyEclipseRun)
        then:
            def task = project.tasks['customRun']
            task.dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            task.httpPort == project.httpPort
    }

    def "Creates new JettyRunWar task"() {
        when:
            project.tasks.create('customRunWar', JettyRunWar)
        then:
            def task = project.tasks['customRunWar']
            task.dependsOn(WarPlugin.WAR_TASK_NAME)
            task.httpPort == project.httpPort
    }
}
