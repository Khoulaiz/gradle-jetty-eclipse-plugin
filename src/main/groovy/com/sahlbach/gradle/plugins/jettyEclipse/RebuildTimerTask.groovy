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
import org.gradle.api.Task
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RebuildTimerTask extends TimerTask {

    public static Logger logger = LoggerFactory.getLogger(JettyEclipseRun)

    private Task task
    private int rebuildIntervalInSeconds
    private GradleConnector gradleConnector
    private Timer timer
    private BuildObserver observer

    RebuildTimerTask(BuildObserver observer, Task task, int rebuildIntervalInSeconds) {
        this.observer = observer
        gradleConnector = GradleConnector.newConnector().forProjectDirectory(new File("."))
        this.task = task
        this.rebuildIntervalInSeconds = rebuildIntervalInSeconds
        timer = new Timer(true)
        Date startDate = new Date(System.currentTimeMillis() + rebuildIntervalInSeconds * 1000)
        timer.schedule(this, startDate, rebuildIntervalInSeconds * 1000)
    }

    void stop() {
        timer.cancel()
    }

    @Override
    public void run() {
        ProjectConnection connection = gradleConnector.connect()
        boolean success = false
        boolean skipped = false
        try {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream()
            ByteArrayOutputStream stderr = new ByteArrayOutputStream()
            BuildLauncher launcher = connection.newBuild().forTasks(task.path)
            launcher.standardOutput = stdout
            launcher.standardError = stderr
            launcher.run()
            String output = stdout.toString()
            skipped = output =~ /$task.path UP-TO-DATE/
            success = output =~ /BUILD SUCCESSFUL/
        } catch (Exception e) {
            success = false
            logger.debug("Build ended with exception: "+e)
        } finally {
            connection.close()
        }
        if(success) {
            if(skipped) {
                observer.notifyBuildWithoutChanges()
            } else {
                observer.notifyBuildWithNewOutput()
            }
        } else {
            observer.notifyBuildFailure()
        }
    }

}
