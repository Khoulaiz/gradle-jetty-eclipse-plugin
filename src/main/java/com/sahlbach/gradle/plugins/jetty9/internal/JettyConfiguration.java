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

package com.sahlbach.gradle.plugins.jetty9.internal;

import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.io.File;
import java.util.List;

public class JettyConfiguration extends PlusConfiguration {
    private static final Logger LOG = Log.getLogger(PlusConfiguration.class);
    private List<File> classPathFiles;
    private File       webXmlFile;

    public JettyConfiguration () {
        super();
    }

    public void setClassPathConfiguration (List<File> classPathFiles) {
        this.classPathFiles = classPathFiles;
    }

    public void setWebXml (File webXmlFile) {
        this.webXmlFile = webXmlFile;
    }

    /**
     * Set up the classloader for the webapp, using the various parts of the Maven project
     *
     * @see org.eclipse.jetty.webapp.Configuration#configureClassLoader()
     */
    public void configureClassLoader () throws Exception {
//        if (classPathFiles != null) {
//            LOG.debug("Setting up classpath ...");
//
//            //put the classes dir and all dependencies into the classpath
//            for (File classPathFile : classPathFiles) {
//                ((WebAppClassLoader) getWebAppContext().getClassLoader()).addClassPath(classPathFile.getCanonicalPath());
//            }
//
//            if (LOG.isDebugEnabled()) {
//                LOG.debug("Classpath = " + LazyList.array2List(((URLClassLoader) getWebAppContext().getClassLoader()).getURLs()));
//            }
//        } else {
//            super.configureClassLoader();
//        }
    }

//    protected URL findWebXml () throws IOException {
//        //if an explicit web.xml file has been set (eg for jetty:run) then use it
//        if (webXmlFile != null && webXmlFile.exists()) {
//            return webXmlFile.toURI().toURL();
//        }
//
//        //if we haven't overridden location of web.xml file, use the
//        //standard way of finding it
//        LOG.debug("Looking for web.xml file in WEB-INF");
//        return super.findWebXml();
//    }

//    public void parseAnnotations() throws Exception {
//        String v = System.getProperty("java.version");
//        String[] version = v.split("\\.");
//        if (version == null) {
//            LOG.info("Unable to determine jvm version, annotations will not be supported");
//            return;
//        }
//        int major = Integer.parseInt(version[0]);
//        int minor = Integer.parseInt(version[1]);
//        if ((major >= 1) && (minor >= 5)) {
//            //TODO it would be nice to be able to re-use the parseAnnotations() method on
//            //the org.eclipse.jetty.annotations.Configuration class, but it's too difficult?
//
//            //able to use annotations on on jdk1.5 and above
//            Class<?> annotationParserClass = Thread.currentThread().getContextClassLoader().loadClass(
//                    "org.eclipse.jetty.annotations.AnnotationParser");
//            Method parseAnnotationsMethod = annotationParserClass.getMethod("parseAnnotations", WebAppContext.class,
//                    Class.class, RunAsCollection.class, InjectionCollection.class, LifeCycleCallbackCollection.class);
//
//            //look thru _servlets
//            Iterator itor = LazyList.iterator(_servlets);
//            while (itor.hasNext()) {
//                ServletHolder holder = (ServletHolder) itor.next();
//                Class servlet = getWebAppContext().loadClass(holder.getClassName());
//                parseAnnotationsMethod.invoke(null, getWebAppContext(), servlet, _runAsCollection, _injections,
//                        _callbacks);
//            }
//
//            //look thru _filters
//            itor = LazyList.iterator(_filters);
//            while (itor.hasNext()) {
//                FilterHolder holder = (FilterHolder) itor.next();
//                Class filter = getWebAppContext().loadClass(holder.getClassName());
//                parseAnnotationsMethod.invoke(null, getWebAppContext(), filter, null, _injections, _callbacks);
//            }
//
//            //look thru _listeners
//            itor = LazyList.iterator(_listeners);
//            while (itor.hasNext()) {
//                Object listener = itor.next();
//                parseAnnotationsMethod.invoke(null, getWebAppContext(), listener.getClass(), null, _injections,
//                        _callbacks);
//            }
//        } else {
//            LOG.info("Annotations are not supported on jvms prior to jdk1.5");
//        }
//    }
}
