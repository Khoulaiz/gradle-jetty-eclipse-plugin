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
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

public class Jetty9PluginTest {
    private final Project project = ProjectBuilder.builder().build()

    @Test
    public void appliesWarPluginAndAddsConventionToProject() {
        new Jetty9Plugin().apply(project)

        assertTrue(project.getPlugins().hasPlugin(WarPlugin))

        assertThat(project.convention.plugins.jetty, instanceOf(JettyPluginConvention))
    }

    @Test
    public void addsTasksToProject() {
        new Jetty9Plugin().apply(project)

        def task = project.tasks[Jetty9Plugin.JETTY_RUN]
        assertThat(task, instanceOf(JettyRun))
        assertThat(task, dependsOn(JavaPlugin.CLASSES_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))

        task = project.tasks[Jetty9Plugin.JETTY_RUN_WAR]
        assertThat(task, instanceOf(JettyRunWar))
        assertThat(task, dependsOn(WarPlugin.WAR_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))

        task = project.tasks[Jetty9Plugin.JETTY_STOP]
        assertThat(task, instanceOf(JettyStop))
        assertThat(task.stopPort, equalTo(project.stopPort))
    }

    @Test
    public void addsMappingToNewJettyTasks() {
        new Jetty9Plugin().apply(project)

        def task = project.tasks.create('customRun', JettyRun)
        assertThat(task, dependsOn(JavaPlugin.CLASSES_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))

        task = project.tasks.create('customWar', JettyRunWar)
        assertThat(task, dependsOn(WarPlugin.WAR_TASK_NAME))
        assertThat(task.httpPort, equalTo(project.httpPort))
    }
}
