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

class JettyEclipsePluginConvention {

    /**
     The port, jetty is going to listen
     */
    Integer httpPort = 8080

    /**
     * Port to listen for stop request via the jettyEclipseStop task
     */
    Integer stopPort = 8090

    /**
     * Key to provide when stopping jetty via the jettyEclipseStop task
     */
    String  stopKey = "stop"

    /**
     * check the war file ever x seconds for changes
     **/
    int scanWarFileInSeconds = 0

    /**
     * true:  the webapp is reloaded automatically in the moment, changes of the war file are detected
     * false: the webapp has to be reloaded manually by pressing ENTER in the console
     */
    boolean automaticReload = false

    /**
     * should the plugin try gradle rebuilds after x seconds to detect changes? 0 == disabled
     * this should be used in conjunction with
     */
    int rebuildIntervalInSeconds = 0

    /**
     * <p> Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests.
     * This is useful when starting the server with the intent to work with it interactively. </p><p> Often, it is desirable to let the server start and continue running subsequent processes in an
     * automated build environment. This can be facilitated by setting daemon to true. </p>
     */
    Boolean daemon = false

}
