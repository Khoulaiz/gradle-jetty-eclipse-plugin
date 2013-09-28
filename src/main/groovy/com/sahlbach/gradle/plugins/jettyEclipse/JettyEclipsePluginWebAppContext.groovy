package com.sahlbach.gradle.plugins.jettyEclipse

import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.log.Logger
import org.eclipse.jetty.webapp.AbstractConfiguration
import org.eclipse.jetty.webapp.Configuration
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.webapp.WebInfConfiguration
import org.eclipse.jetty.webapp.WebXmlConfiguration

/**
 * User: ace
 * Date: 27.09.13
 * Time: 01:58
 */
class JettyEclipsePluginWebAppContext extends WebAppContext {
    private static final Logger logger = Log.getLogger(WebAppContext.class);
    private List<File> classpathFiles;
    private File       jettyEnvXmlFile;
    private File       webXmlFile;
    private WebXmlConfiguration      webXmlConfiguration = new WebXmlConfiguration();
    private WebInfConfiguration      webInfConfig        = new WebInfConfiguration();
    private EnvConfiguration         envConfig           = new EnvConfiguration();
    private PlusConfiguration        plusConfiguration   = new PlusConfiguration();
    private JettyWebXmlConfiguration jettyWebConfig      = new JettyWebXmlConfiguration();
    private List<AbstractConfiguration> configs          = [webXmlConfiguration,
            webInfConfig,
            envConfig,
            plusConfiguration,
            jettyWebConfig] as AbstractConfiguration[];

    JettyEclipsePluginWebAppContext () {
        super();
        String v = System.getProperty("java.version");
        String[] version = v.split("\\.");
        if (version == null) {
            logger.info("Unable to determine jvm version, annotations will not be supported");
        } else {
            int major = Integer.parseInt(version[0]);
            int minor = Integer.parseInt(version[1]);
            if ((major >= 1) && (minor >= 5)) {
                AbstractConfiguration annotationConfig = new AnnotationConfiguration();
                configs.add(4,annotationConfig);
            }
        }
        configurations = configs.toArray(new Configuration[configs.size()]);
    }

    @Override
    void preConfigure () throws Exception {
        if (webXmlFile != null)
            descriptor = webXmlFile.canonicalPath;
        StringBuilder extraClasspath = new StringBuilder();
        if(classpathFiles != null) {
            for (File classpathFile : classpathFiles) {
                if(extraClasspath.length() > 0)
                    extraClasspath.append(';');
                extraClasspath.append(classpathFile.canonicalPath);
            }
            setExtraClasspath(extraClasspath.toString());
        }
        super.preConfigure();
    }

    @Override
    public void configure () throws Exception {
        try {
            if (this.jettyEnvXmlFile != null) {
                envConfig.jettyEnvXml = this.jettyEnvXmlFile.toURI().toURL();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        super.configure();
    }
}
