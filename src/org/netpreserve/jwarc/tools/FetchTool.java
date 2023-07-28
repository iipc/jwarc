package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.FetchOptions;
import org.netpreserve.jwarc.WarcWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FetchTool {
    public static void main(String[] args) throws IOException, URISyntaxException {
        FetchOptions options = new FetchOptions();
        List<URI> urls = new ArrayList<>();
        Path outputFile = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    System.out.println("Usage: jwarc fetch [options] url...");
                    System.out.println("Fetches a URL while writing the request and response as WARC records");
                    System.out.println();
                    System.out.println("Options:");
                    System.out.println("  -A, --user-agent STRING    Sets the User-Agent header");
                    System.out.println("      --read-timeout MILLIS  Sets the socket read timeout");
                    System.out.println("      --max-length BYTES     Truncate response after BYTES received");
                    System.out.println("      --max-time MILLIS      Truncate response after MILLIS elapsed");
                    System.out.println("  -o, --output-file FILE     Write WARC records to FILE instead of stdout");
                    System.out.println();
                    System.exit(0);
                    break;
                case "-A":
                case "--user-agent":
                    options.userAgent(args[++i]);
                    break;
                case "--read-timeout":
                    options.readTimeout(Integer.parseInt(args[++i]));
                    break;
                case "--max-length":
                    options.maxLength(Integer.parseInt(args[++i]));
                    break;
                case "--max-time":
                    options.maxTime(Integer.parseInt(args[++i]));
                    break;
                case "-o":
                case "--output-file":
                    outputFile = Paths.get(args[++i]);
                    break;
                default:
                    if (args[i].startsWith("-")) {
                        System.err.println("Unknown option: " + args[i]);
                        System.exit(1);
                    }
                    urls.add(new URI(args[i]));
            }
        }
        if (urls.isEmpty()) {
            System.err.println("No URLs specified. Try: jwarc fetch --help");
            System.exit(1);
        }
        try (WarcWriter writer = outputFile == null ? new WarcWriter(System.out) : new WarcWriter(outputFile)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    // Ensure current progress is written before exiting.
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "FetchToolShutdownHook"));
            for (URI url : urls) {
                writer.fetch(url, options);
            }
        }
    }
}
