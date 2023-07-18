/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.tools;


import org.netpreserve.jwarc.cdx.CdxFormat;
import org.netpreserve.jwarc.cdx.CdxProcessor;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CdxTool {
    public static void main(String[] args) throws IOException {
        boolean printHeader = true;
        boolean postAppend = false;
        List<Path> files = new ArrayList<>();
        CdxFormat.Builder cdxFormatBuilder = new CdxFormat.Builder();
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
                    cdxFormatBuilder.revisistsIncluded();
                    break;
                case "-w":
                case "--warc-full-path":
                    cdxFormatBuilder.fullFilePath();
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
        CdxProcessor.process(printHeader, postAppend, files, cdxFormatBuilder,System.out); //Output to System.out
    }
}
