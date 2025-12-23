/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.tools;


import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcRevisit;
import org.netpreserve.jwarc.cdx.CdxFormat;
import org.netpreserve.jwarc.cdx.CdxWriter;


import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CdxTool {
    public static void main(String[] args) throws IOException {
        List<Path> files = new ArrayList<>();
        CdxFormat format = CdxFormat.CDX11;
        String legend = null;
        boolean printHeader = true;
        boolean fullFilePath = false;
        boolean postAppend = false;
        boolean digestUnchanged = false;
        boolean sort = false;
        Predicate<WarcRecord> filter = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                switch (args[i]) {
                case "-f":
                case "--format":
                    String formatName = args[++i];
                    switch (formatName) {
                    case "CDX9":
                        format = CdxFormat.CDX9;
                        break;
                    case "CDX10":
                        format = CdxFormat.CDX10;
                        break;
                    case "CDX11":
                        format = CdxFormat.CDX11;
                        break;
                    case "CDXJ":
                        format = CdxFormat.CDXJ;
                        printHeader = false;
                        break;
                    default:
                        legend = formatName;
                        break;
                    }
                    break;
                case "-h":
                case "--help":
                    System.out.println("Usage: jwarc cdx [--format LEGEND] warc-files...");
                    System.out.println();
                    System.out.println("  -d, --digest-unchanged   Include records with unchanged digest");
                    System.out.println("  -f, --format LEGEND      CDX format may be CDX9, CDX11 or a custom legend");
                    System.out.println("      --no-header          Don't print the CDX header line");
                    System.out.println("  -p, --post-append        Append the request body to the urlkey field");
                    System.out.println("      --revisits-excluded  Don't index revisit records");
                    System.out.println("  -w, --warc-full-path     Use absolute paths for the filename field");
                    return;
                case "--no-header":
                    printHeader = false;
                    break;
                case "-p":
                case "--post-append":
                    postAppend = true;
                    break;
                case "-d":
                case "--digest-unchanged":
                    digestUnchanged = true;
                    break;
                case "-r":
                case "--revisits-included":
                    filter = null;
                    break;
                case "--revisits-excluded":
                    filter = record -> !(record instanceof WarcRevisit);
                    break;
                case "-s":
                case "--sort":
                    sort = true;
                    break;
                case "-w":
                case "--warc-full-path":
                    fullFilePath = true;
                    break;                         
                default:
                    System.err.println("Unrecognized option: " + args[i]);
                    System.err.println("Usage: jwarc cdx [--format LEGEND] warc-files...");
                    System.exit(1);
                    return;
                }
            } else {
                //noinspection JvmTaintAnalysis
                files.add(Paths.get(args[i]));
            }
        }

        CdxFormat.Builder builder = new CdxFormat.Builder(format);
        if (legend != null) builder.legend(legend);
        if (digestUnchanged) builder.digestUnchanged();
        format = builder.build();

        try (CdxWriter cdxWriter = new CdxWriter(new OutputStreamWriter(System.out))) {
            cdxWriter.onWarning(System.err::println);
            cdxWriter.setFormat(format);
            cdxWriter.setPostAppend(postAppend);
            cdxWriter.setRecordFilter(filter);
            cdxWriter.setSort(sort);

            if (printHeader) cdxWriter.writeHeaderLine();
            cdxWriter.process(files, fullFilePath);
        }
    }

    static void cdxj(String[] rest) throws IOException {
        main(Stream.concat(Stream.of("--format", "CDXJ"), Arrays.stream(rest))
                .toArray(String[]::new));
    }
}
