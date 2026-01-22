package org.netpreserve.jwarc.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
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

    static boolean hasHelpFlag(String[] args) {
        return Arrays.stream(args).anyMatch(arg -> arg.equals("-h") || arg.equals("--help") || arg.equals("-?"));
    }

    static void showUsage(String[] args, int minArgs, Class<?> cls, String arguments, String description) {
        if (hasHelpFlag(args) || args.length < minArgs) {
            System.err.println("");
            System.err.println(cls.getSimpleName() + " [-h] " + arguments);
            System.err.println("");
            System.err.println(description);
            System.err.println("");
            System.err.println("Options:");
            System.err.println("");
            System.err.println(" -h / --help\tshow usage message and exit");
            System.err.println("");
            System.exit(0);
        }
    }
}
