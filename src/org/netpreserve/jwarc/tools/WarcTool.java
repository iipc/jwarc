package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.cdx.CdxFormat;

import java.util.ArrayList;
import java.util.Arrays;

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
            case "cdxj":
                CdxTool.cdxj(rest);
                break;
            case "dedupe":
                DedupeTool.main(rest);
                break;
            case "extract":
                ExtractTool.main(rest);
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
            case "saveback":
                SavebackTool.main(rest);
                break;
            case "screenshot":
                ScreenshotTool.main(rest);
                break;
            case "serve":
                ServeTool.main(rest);
                break;
            case "stats":
                StatsTool.main(rest);
                break;
            case "validate":
                ValidateTool.main(rest);
                break;
            case "--version":
            case "version":
                version();
                break;
            case "view":
                ViewTool.main(rest);
                break;
            case "wacz":
                WaczTool.main(rest);
                break;
            default:
                System.err.println("jwarc: '" + args[0] + "' is not a jwarc command. See 'jwarc help'.");
                System.exit(1);
        }
    }

    private static void usage() {
        System.out.println("usage: jwarc <command> [args]...");
        System.out.println();
        System.out.println("Commands:");
        System.out.println();
        System.out.println("  cdx         List records in CDX format");
        System.out.println("  cdxj        List records in CDXJ format");
        System.out.println("  dedupe      Deduplicate records by looking up a CDX server");
        System.out.println("  extract     Extract record by offset");
        System.out.println("  fetch       Download a URL recording the request and response");
        System.out.println("  filter      Copy records that match a given filter expression");
        System.out.println("  ls          List records in WARC file(s)");
        System.out.println("  record      Fetch a page and subresources using headless Chrome");
        System.out.println("  recorder    Run a recording proxy");
        System.out.println("  saveback    Saves wayback-style replayed pages as WARC records");
        System.out.println("  screenshot  Take a screenshot of each page in the given WARCs");
        System.out.println("  serve       Serve WARC files with a basic replay server/proxy");
        System.out.println("  stats       Print statistics about WARC and CDX files");
        System.out.println("  validate    Validate WARC or ARC files");
        System.out.println("  version     Print version information");
        System.out.println("  view        View WARC file with an interactive terminal UI");
        //System.out.println("  wacz        Package WARC files into a WACZ file (EXPERIMENTAL)");
    }

    private static void version() {
        String version = Utils.getJwarcVersion();
        System.out.println("jwarc " + (version == null ? "unknown version" : version));
        System.out.println(System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        System.out.println(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
    }
}
