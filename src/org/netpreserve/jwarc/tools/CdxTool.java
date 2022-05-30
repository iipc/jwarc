/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;
import org.netpreserve.jwarc.cdx.CdxFormat;
import org.netpreserve.jwarc.cdx.CdxRequestEncoder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CdxTool {
    public static void main(String[] args) throws IOException {
        boolean printHeader = true;
        boolean postAppend = false;
        CdxFormat cdxFormat = CdxFormat.CDX11;
        List<Path> files = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                switch (args[i]) {
                    case "-f":
                    case "--format":
                        String format = args[++i];
                        switch (format) {
                            case "CDX9":
                                cdxFormat = CdxFormat.CDX9;
                                break;
                            case "CDX10":
                                cdxFormat = CdxFormat.CDX10;
                                break;
                            case "CDX11":
                                cdxFormat = CdxFormat.CDX11;
                                break;
                            default:
                                cdxFormat = new CdxFormat(format);
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

        if (printHeader) {
            System.out.println(" CDX " + cdxFormat.legend());
        }

        for (Path file: files) {
            try (WarcReader reader = new WarcReader(file)) {
                reader.onWarning(System.err::println);
                WarcRecord record = reader.next().orElse(null);
                String filename = file.getFileName().toString();
                while (record != null) {
                    try {
                        if ((record instanceof WarcResponse || record instanceof WarcResource) &&
                                ((WarcCaptureRecord) record).payload().isPresent()) {
                            long position = reader.position();
                            WarcCaptureRecord capture = (WarcCaptureRecord) record;
                            URI id = record.version().getProtocol().equals("ARC") ? null : record.id();

                            // advance to the next record so we can calculate the length
                            record = reader.next().orElse(null);
                            long length = reader.position() - position;

                            String urlKey = null;
                            if (postAppend) {
                                // check for a corresponding request record
                                while (urlKey == null && record instanceof WarcCaptureRecord
                                        && ((WarcCaptureRecord) record).concurrentTo().contains(id)) {
                                    if (record instanceof WarcRequest) {
                                        HttpRequest httpRequest = ((WarcRequest) record).http();
                                        String encodedRequest = CdxRequestEncoder.encode(httpRequest);
                                        if (encodedRequest != null) {
                                            String rawUrlKey = capture.target() +
                                                    (capture.target().contains("?") ? '&' : '?')
                                                    + encodedRequest;
                                            urlKey = URIs.toNormalizedSurt(rawUrlKey);
                                        }
                                    }

                                    record = reader.next().orElse(null);
                                }
                            }

                            System.out.println(cdxFormat.format(capture, filename, position, length, urlKey));
                        } else {
                            record = reader.next().orElse(null);
                        }
                    } catch (ParsingException e) {
                        System.err.println("ParsingException at record " + reader.position() + ": " + e.getMessage());
                        record = reader.next().orElse(null);
                    }
                }
            }
        }
    }
}
