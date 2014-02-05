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
import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.log.Logger
import org.eclipse.jetty.webapp.*

class JettyEclipsePluginWebAppContext extends WebAppContext {
    private static final Logger logger = Log.getLogger(WebAppContext.class);
    private List<File> classpathFiles;
    private File       jettyEnvXmlFile;
    private File       webXmlFile;
    private WebInfConfiguration      webInfConfig        = new WebInfConfiguration()
    private WebXmlConfiguration      webXmlConfiguration = new WebXmlConfiguration()
    private MetaInfConfiguration    metaInfConfiguration = new MetaInfConfiguration()
    private FragmentConfiguration  fragmentConfiguration = new FragmentConfiguration()
    private EnvConfiguration         envConfig           = new EnvConfiguration()
    private PlusConfiguration        plusConfiguration   = new PlusConfiguration()
    private JettyWebXmlConfiguration jettyWebConfig      = new JettyWebXmlConfiguration()
    private List<AbstractConfiguration> configs          = [webInfConfig,
                                                            webXmlConfiguration,
                                                            metaInfConfiguration,
                                                            fragmentConfiguration,
                                                            envConfig,
                                                            plusConfiguration,
                                                            jettyWebConfig] as AbstractConfiguration[]

    JettyEclipsePluginWebAppContext (boolean skipAnnotations) {
        super();
        if(!skipAnnotations) {
            String v = System.getProperty("java.version")
            String[] version = v.split("\\.")
            if (version == null) {
                logger.info("Unable to determine jvm version, annotations will not be supported")
            } else {
                int major = Integer.parseInt(version[0])
                int minor = Integer.parseInt(version[1])
                if ((major >= 1) && (minor >= 5)) {
                    AbstractConfiguration annotationConfig = new AnnotationConfiguration()
                    configs.add(4,annotationConfig)
                }
            }
        }
        configurations = configs.toArray(new Configuration[configs.size()])
    }

    @Override
    void preConfigure () throws Exception {
        if (webXmlFile != null)
            descriptor = webXmlFile.canonicalPath
        StringBuilder extraClasspath = new StringBuilder()
        if(classpathFiles != null) {
            for (File classpathFile : classpathFiles) {
                if(extraClasspath.length() > 0)
                    extraClasspath.append(';')
                extraClasspath.append(classpathFile.canonicalPath)
            }
            setExtraClasspath(extraClasspath.toString())
        }
        super.preConfigure()
    }

    @Override
    public void configure () throws Exception {
        try {
            if (this.jettyEnvXmlFile != null) {
                envConfig.jettyEnvXml = this.jettyEnvXmlFile.toURI().toURL()
            }
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
        super.configure()
    }
}
