package org.netpreserve.jwarc.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Properties;

public class WarcTool {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "cdx":
                CdxTool.main(rest);
                break;
            case "fetch":
                FetchTool.main(rest);
                break;
            case "filter":
                FilterTool.main(rest);
                break;
            case "-h":
            case "--help":
            case "help":
                usage();
                break;
            case "ls":
                ListTool.main(rest);
                break;
            case "record":
                RecordTool.main(rest);
                break;
            case "recorder":
                RecorderTool.main(rest);
                break;
            case "screenshot":
                ScreenshotTool.main(rest);
                break;
            case "serve":
                ServeTool.main(rest);
                break;
            case "--version":
            case "version":
                version();
                break;
            default:
                System.err.println("jwarc: '" + args[0] + "' is not a jwarc command. See 'jwarc help'.");
                System.exit(1);
        }
    }

    private static void usage() {
        System.out.println("usage: jwarc <command> [args]...");
        System.out.println("");
        System.out.println("Commands:");
        System.out.println("");
        System.out.println("  cdx         List records in CDX format");
        System.out.println("  fetch       Download a URL recording the request and response");
        System.out.println("  filter      Copy records that match a given filter expression");
        System.out.println("  ls          List records in WARC file(s)");
        System.out.println("  record      Fetch a page and subresources using headless Chrome");
        System.out.println("  recorder    Run a recording proxy");
        System.out.println("  screenshot  Take a screenshot of each page in the given WARCs");
        System.out.println("  serve       Serve WARC files with a basic replay server/proxy");
        System.out.println("  version     Print version information");
    }

    private static void version() {
        Properties properties = new Properties();
        URL resource = WarcTool.class.getResource("/META-INF/maven/org.netpreserve/jwarc/pom.properties");
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                properties.load(stream);
            } catch (IOException e) {
                // alas!
            }
        }
        String version = properties.getProperty("version", "unknown version");
        System.out.println("jwarc " + version);
        System.out.println(System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        System.out.println(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
    }
}
