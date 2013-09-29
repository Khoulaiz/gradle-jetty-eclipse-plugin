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
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.LoginService
import org.eclipse.jetty.security.SecurityHandler
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.RequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.RequestLogHandler
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * User: ace
 * Date: 27.09.13
 * Time: 00:38
 */
class JettyEclipsePluginServer {
    private static final Logger logger = LoggerFactory.getLogger(JettyEclipsePluginServer.class)

    static final int DEFAULT_MAX_IDLE_TIME = 30000
    private Server                   server
    private ContextHandlerCollection contexts
    private HandlerCollection handlers
    private RequestLogHandler requestLogHandler
    private DefaultHandler    defaultHandler

    public RequestLog requestLog

    JettyEclipsePluginServer() {
        this.server = new Server()
        this.server.stopAtShutdown = true
        //make sure Jetty does not use URLConnection caches with the plugin
        Resource.defaultUseCaches = false
    }

    void setConnectors(Connector[] connectors) {
        if (connectors == null || connectors.length == 0) {
            return
        }
        connectors.each { connector ->
            logger.debug("Setting Connector: " + connector.class.name)
            this.server.addConnector(connector)
        }
    }

    public Connector[] getConnectors () {
        return this.server.connectors
    }

    public void setLoginServices (LoginService[] services) throws Exception {
        if (services != null) {
            SecurityHandler sHandler
            if (this.server.getChildHandlerByClass(SecurityHandler) != null) {
                sHandler = this.server.getChildHandlerByClass(SecurityHandler)
            } else {
                sHandler = new ConstraintSecurityHandler()
            }
            services.each { service ->
                sHandler.addBean(service)
            }
            this.handlers.addHandler(sHandler)
        }
    }

    public LoginService[] getLoginServices () {
        if (this.server.getChildHandlerByClass(SecurityHandler) == null) {
            return new LoginService[0]
        } else {
            return this.server.getChildHandlerByClass(SecurityHandler).beans as LoginService[]
        }
    }

    public void start() throws Exception {
        logger.info("Starting jetty " + this.server.class.package.implementationVersion + " ...")
        this.server.start()
    }

    public Object getProxiedObject() {
        return this.server
    }

    public void addWebApplication(WebAppContext webapp) throws Exception {
        contexts.addHandler(webapp)
    }

    /**
     * Set up the handler structure to receive a webapp. Also put in a DefaultHandler so we get a nice page than a 404
     * if we hit the root and the webapp's context isn't at root.
     */
    public void configureHandlers() throws Exception {
        this.defaultHandler = new DefaultHandler()
        this.requestLogHandler = new RequestLogHandler()
        if (this.requestLog != null) {
            this.requestLogHandler.requestLog = this.requestLog
        }

        this.contexts = server.getChildHandlerByClass(ContextHandlerCollection.class)
        if (this.contexts == null) {
            this.contexts = new ContextHandlerCollection()
            this.handlers = server.getChildHandlerByClass(HandlerCollection.class)
            if (this.handlers == null) {
                this.handlers = new HandlerCollection()
                this.server.handler = handlers
                this.handlers.handlers = [this.contexts, this.defaultHandler, this.requestLogHandler]
            } else {
                this.handlers.addHandler(this.contexts)
            }
        }
    }

    public Connector createDefaultConnector(int port) throws Exception {
        ServerConnector connector = new ServerConnector(this.server)
        connector.port = port
        connector.idleTimeout = DEFAULT_MAX_IDLE_TIME
        return connector
    }

    public void join() throws Exception {
        this.server.threadPool.join()
    }

}
