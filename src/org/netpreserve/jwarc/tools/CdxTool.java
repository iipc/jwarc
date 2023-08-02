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
import java.util.List;
import java.util.function.Predicate;

public class CdxTool {
    public static void main(String[] args) throws IOException {
        List<Path> files = new ArrayList<>();
        CdxFormat.Builder cdxFormatBuilder = new CdxFormat.Builder();
        boolean printHeader = true;
        boolean fullFilePath = false;
        boolean postAppend = false;
        Predicate<WarcRecord> filter = record -> !(record instanceof WarcRevisit);
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                switch (args[i]) {
                case "-f":
                case "--format":
                    String format = args[++i];
                    switch (format) {
                    case "CDX9":
                        cdxFormatBuilder.legend(CdxFormat.CDX9_LEGEND);
                        break;
                    case "CDX10":
                        cdxFormatBuilder.legend(CdxFormat.CDX10_LEGEND);
                        break;
                    case "CDX11":
                        cdxFormatBuilder.legend(CdxFormat.CDX11_LEGEND);
                        break;
                    default:
                        cdxFormatBuilder.legend(format);
                        break;
                    }
                    break;
                case "-h":
                case "--help":
                    System.out.println("Usage: jwarc cdx [--format LEGEND] warc-files...");
                    System.out.println();
                    System.out.println("  -f, --format LEGEND  CDX format may be CDX9, CDX11 or a custom legend");
                    System.out.println("      --no-header      Don't print the CDX header line");
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
                    cdxFormatBuilder.digestUnchanged();
                    break;
                case "-r":
                case "--revisits-included":
                    filter = null;
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
                files.add(Paths.get(args[i]));
            }
        }

        try (CdxWriter cdxWriter = new CdxWriter(new OutputStreamWriter(System.out))) {
            cdxWriter.onWarning(System.err::println);
            cdxWriter.setFormat(cdxFormatBuilder.build());
            cdxWriter.setPostAppend(postAppend);
            cdxWriter.setRecordFilter(filter);

            if (printHeader) cdxWriter.writeHeaderLine();
            cdxWriter.process(files, fullFilePath);
        }
    }
}
