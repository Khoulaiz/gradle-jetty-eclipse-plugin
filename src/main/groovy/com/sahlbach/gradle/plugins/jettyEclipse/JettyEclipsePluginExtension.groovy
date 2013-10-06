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

class JettyEclipsePluginExtension {

    /**
     The port, jetty is going to listen
     */
    int httpPort

    /**
     * Port to listen for stop request via the jettyEclipseStop task
     */
    int stopPort

    /**
     * Key to provide when stopping jetty via the jettyEclipseStop task
     */
    String  stopKey

    /**
     * War File to use for the web app
     */
    File warFile

    /**
     * check the war file ever x seconds for changes
     **/
    int scanIntervalInSeconds

    /**
     * The context path for the webapp.
     */
    String contextPath

    /**
     * A webdefault.xml file to use instead of the default for the webapp. Optional.
     */
    File webDefaultXml

    /**
     * A web.xml file to be applied AFTER the webapp's web.xml file. Useful for applying different build profiles, eg test, production etc. Optional.
     */
    File overrideWebXml

    /**
     * Location of a jetty XML configuration file whose contents will be applied before any plugin configuration. Optional.
     */
    File jettyConfig

    /**
     * true:  the webapp is reloaded automatically in the moment, changes of the war file are detected
     * false: the webapp has to be reloaded manually by pressing ENTER in the console
     */
    boolean automaticReload

    /**
     * should the plugin try gradle rebuilds after x seconds to detect changes? 0 == disabled
     */
    int rebuildIntervalInSeconds

    /**
     * true: don't scan for servlet annotations
     */
    boolean skipAnnotations

    /**
     * The task to start for the background rebuild
     */
    Task rebuildTask

    /**
     * A NCSA RequestLog to use for the webapp at runtime.
     */
    File requestLog

    /**
     * User File to use for a convenient way to setup a security service
     * A file containing lines with the following content "user:pwd[,role]" eg. "ace: joshua,admin"
     * Will be used with a BasicAuthenticator for the server.
     */
    File passwordFile

    /**
     * list of additional runtime jars for the classpath
     */
    Iterable<File> additionalRuntimeJars = new ArrayList<File>()

    /**
     * <p> Determines whether or not the server blocks when started. The default behavior (daemon = false) will cause the server to pause other processes while it continues to handle web requests.
     * This is useful when starting the server with the intent to work with it interactively. </p><p> Often, it is desirable to let the server start and continue running subsequent processes in an
     * automated build environment. This can be facilitated by setting daemon to true. </p>
     */
    boolean daemon = false
}
