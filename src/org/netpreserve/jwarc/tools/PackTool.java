/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2026 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcResource;
import org.netpreserve.jwarc.WarcWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PackTool {
    public static void main(String[] args) throws IOException, URISyntaxException {
        Path outputFile = null;
        List<String> positionalArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    usage();
                    return;
                case "-o":
                case "--output-file":
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing filename after " + args[i - 1]);
                    }
                    outputFile = Paths.get(args[i]);
                    break;
                default:
                    if (args[i].startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + args[i]);
                    }
                    positionalArgs.add(args[i]);
            }
        }

        if (positionalArgs.size() < 2) {
            throw new IllegalArgumentException("A base URL and at least one file are required");
        }

        URI baseUrl = new URI(positionalArgs.get(0));
        try (WarcWriter writer = outputFile == null ? new WarcWriter(System.out) : new WarcWriter(outputFile)) {
            for (int i = 1; i < positionalArgs.size(); i++) {
                pack(writer, baseUrl, Paths.get(positionalArgs.get(i)));
            }
        }
    }

    static void pack(WarcWriter writer, URI baseUrl, Path file) throws IOException, URISyntaxException {
        String filename = file.getFileName().toString();
        URI relativeUrl = new URI(null, null, filename, null);
        String mimeType = URLConnection.guessContentTypeFromName(filename);
        MediaType contentType = mimeType == null ? MediaType.OCTET_STREAM : MediaType.parse(mimeType);

        try (ReadableByteChannel body = Files.newByteChannel(file)) {
            writer.write(new WarcResource.Builder(baseUrl.resolve(relativeUrl))
                    .date(Files.getLastModifiedTime(file).toInstant())
                    .body(contentType, body, Files.size(file))
                    .build());
        }
    }

    private static void usage() {
        System.out.println("Usage: jwarc pack [options] base-url file...");
        System.out.println("Packs local files as WARC resource records");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output-file FILE     Write WARC records to FILE instead of stdout");
    }
}
