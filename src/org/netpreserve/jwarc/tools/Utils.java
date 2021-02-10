package org.netpreserve.jwarc.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

class Utils {
    static String getJwarcVersion() {
        Properties properties = new Properties();
        URL resource = WarcTool.class.getResource("/META-INF/maven/org.netpreserve/jwarc/pom.properties");
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                properties.load(stream);
            } catch (IOException e) {
                // alas!
            }
        }
        return properties.getProperty("version");
    }
}
