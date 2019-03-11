package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class FilterTool {
    public static void main(String[] args) throws Exception {
        try {
            String[] files;
            if (args.length == 0) {
                System.err.println("Usage: jwarc filter <expression> [warc-file]...");
                System.err.println("  e.g. jwarc filter 'warc-type == \"response\" && http:content-type =~ \"image/.*\" && :status == 200' example.warc");
                System.exit(1);
                return;
            } else if (args.length > 1) {
                files = Arrays.copyOfRange(args, 1, args.length);
            } else {
                if (System.console() != null) {
                    System.err.println("Warning: No input files specified, reading from STDIN");
                }
                files = new String[]{"-"};
            }
            WarcFilter filter = WarcFilter.compile(args[0]);
            try (WarcWriter writer = new WarcWriter(System.out)) {
                for (String file : files) {
                    try (WarcReader reader = file.equals("-") ? new WarcReader(System.in) : new WarcReader(Paths.get(file))) {
                        filterRecords(filter, writer, reader);
                    }
                }
            }
        } catch (WarcFilterException e) {
            System.err.println(e.prettyPrint());
            System.exit(2);
        }
    }

    private static void filterRecords(WarcFilter filter, WarcWriter writer, WarcReader reader) throws IOException {
        for (WarcRecord record : reader) {
            if (filter.test(record)) {
                writer.write(record);
            }
        }
    }
}
