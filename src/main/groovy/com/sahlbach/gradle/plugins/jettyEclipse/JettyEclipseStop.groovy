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
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Stops the embedded Jetty web container, if it is running.
 */
class JettyEclipseStop extends DefaultTask {
    private static Logger logger = LoggerFactory.getLogger(JettyEclipseStop);

    /**
     * Port to listen to stop jetty on.
     */
    @Input
    Integer stopPort

    /**
     * Key to provide when stopping jetty.
     */
    @Input
    String stopKey

    @TaskAction
    void stop() {
        initFromExtension()
        if (stopPort == null) {
            throw new InvalidUserDataException("Please specify a valid port")
        }
        if (stopKey == null) {
            throw new InvalidUserDataException("Please specify a valid stopKey")
        }

        ProgressLogger progressLogger = services.get(ProgressLoggerFactory).newOperation(JettyEclipseStop)
        progressLogger.description = "Stop Jetty server"
        progressLogger.shortDescription = "Stopping Jetty"
        progressLogger.started()
        try {
            Socket s = new Socket(InetAddress.getByName("127.0.0.1"), stopPort)
            s.setSoLinger(false, 0)

            OutputStream out = s.outputStream
            out.write((stopKey + "\r\nstop\r\n").bytes)
            out.flush()
            s.close()
        } catch (ConnectException e) {
            logger.info("Jetty not running!")
        } catch (Exception e) {
            logger.error("Exception during stopping", e)
        } finally {
            progressLogger.completed()
        }

    }

    void initFromExtension() {
        JettyEclipsePluginExtension extension = project.extensions
                                                       .getByName(JettyEclipsePlugin.JETTY_ECLIPSE_EXTENSION) as JettyEclipsePluginExtension
        if(stopPort == null)
            stopPort = extension.stopPort

        if(stopKey == null)
            stopKey = extension.stopKey
    }
}
