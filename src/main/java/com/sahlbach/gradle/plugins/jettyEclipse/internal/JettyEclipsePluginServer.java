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

package com.sahlbach.gradle.plugins.jettyEclipse.internal;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JettyEclipsePluginServer <p/> Jetty Eclipse version of a wrapper for the Server class.
 */
public class JettyEclipsePluginServer implements JettyPluginServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyEclipsePluginServer.class);

    public static final int DEFAULT_MAX_IDLE_TIME = 30000;
    private Server                   server;
    private ContextHandlerCollection contexts; //the list of ContextHandlers
    HandlerCollection handlers; //the list of lists of Handlers
    private RequestLogHandler requestLogHandler; //the request log handler
    private DefaultHandler    defaultHandler; //default handler

    private RequestLog requestLog; //the particular request log implementation

    public JettyEclipsePluginServer () {
        this.server = new Server();
        this.server.setStopAtShutdown(true);
        //make sure Jetty does not use URLConnection caches with the plugin
        Resource.setDefaultUseCaches(false);
    }

    /**
     * @see JettyEclipsePluginServer#setConnectors(Object[])
     */
    public void setConnectors (Object[] connectors) {
        if (connectors == null || connectors.length == 0) {
            return;
        }

        for (int i = 0; i < connectors.length; i++) {
            Connector connector = (Connector) connectors[i];
            LOGGER.debug("Setting Connector: " + connector.getClass().getName());
            this.server.addConnector(connector);
        }
    }

    /**
     * @see org.gradle.api.plugins.jetty.internal.JettyPluginServer#getConnectors()
     */
    public Object[] getConnectors () {
        return this.server.getConnectors();
    }

    @Override
    public void setLoginServices (Object[] services) throws Exception {
        if (services != null) {
            SecurityHandler sHandler;
            if (this.server.getChildHandlerByClass(SecurityHandler.class) != null) {
                sHandler = this.server.getChildHandlerByClass(SecurityHandler.class);
            } else {
                sHandler = new ConstraintSecurityHandler();
            }
            for (Object service : services) {
                sHandler.addBean(service);
            }
            this.handlers.addHandler(sHandler);
        }
    }

    @Override
    public Object[] getLoginServices () {
        if (this.server.getChildHandlerByClass(SecurityHandler.class) == null) {
            return new Object[0];
        } else {
            return this.server.getChildHandlerByClass(SecurityHandler.class).getBeans().toArray();
        }
    }

    public void setRequestLog(Object requestLog) {
        this.requestLog = (RequestLog) requestLog;
    }

    public Object getRequestLog() {
        return this.requestLog;
    }

    /**
     * @see org.gradle.api.plugins.jetty.internal.JettyPluginServer#start()
     */
    public void start() throws Exception {
        LOGGER.info("Starting jetty " + this.server.getClass().getPackage().getImplementationVersion() + " ...");
        this.server.start();
    }

    /**
     * @see org.gradle.api.plugins.jetty.internal.Proxy#getProxiedObject()
     */
    public Object getProxiedObject() {
        return this.server;
    }

    /**
     * @see JettyEclipsePluginServer#addWebApplication
     */
    public void addWebApplication(WebAppContext webapp) throws Exception {
        contexts.addHandler(webapp);
    }

    /**
     * Set up the handler structure to receive a webapp. Also put in a DefaultHandler so we get a nice page than a 404
     * if we hit the root and the webapp's context isn't at root.
     */
    public void configureHandlers() throws Exception {
        this.defaultHandler = new DefaultHandler();
        this.requestLogHandler = new RequestLogHandler();
        if (this.requestLog != null) {
            this.requestLogHandler.setRequestLog(this.requestLog);
        }

        this.contexts = (ContextHandlerCollection) server.getChildHandlerByClass(ContextHandlerCollection.class);
        if (this.contexts == null) {
            this.contexts = new ContextHandlerCollection();
            this.handlers = (HandlerCollection) server.getChildHandlerByClass(HandlerCollection.class);
            if (this.handlers == null) {
                this.handlers = new HandlerCollection();
                this.server.setHandler(handlers);
                this.handlers.setHandlers(new Handler[]{this.contexts, this.defaultHandler, this.requestLogHandler});
            } else {
                this.handlers.addHandler(this.contexts);
            }
        }
    }

    public Object createDefaultConnector(int port) throws Exception {
        ServerConnector connector = new ServerConnector(this.server);
        connector.setPort(port);
        connector.setIdleTimeout(DEFAULT_MAX_IDLE_TIME);

        return connector;
    }

    public void join() throws Exception {
        this.server.getThreadPool().join();
    }
}
