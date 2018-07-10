/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EncodedWords {
    static String test = "WARC/1.0\r\n" +
            "Encoded: =?iso-8859-1?q?this=20is=20some=20text?=\r\n" +
            "Folded:   a   \r\n" +
            "     b     c \r\n" +
            "\t   d  \r\n" +
            "\r\n";

    public static void main(String args[]) throws IOException {
        System.out.println("wa-commons");
        try (ArchiveReader reader = WARCReaderFactory.get("test.warc", new ByteArrayInputStream(test.getBytes(StandardCharsets.US_ASCII)), false)) {
            ArchiveRecord record = reader.get();
            System.out.println(record.getHeader().getHeaderValue("Folded"));
            System.out.println(record.getHeader().getHeaderValue("Encoded"));
        }

        System.out.println("\njwat");
        try (WarcReader reader = WarcReaderFactory.getReader(new ByteArrayInputStream(test.getBytes(StandardCharsets.US_ASCII)))) {
            WarcRecord record = reader.getNextRecord();
            System.out.println(record.getHeader("Folded").value);
            System.out.println(record.getHeader("Encoded").value);
        }

        System.out.println("\njwarc");
        try (org.netpreserve.jwarc.WarcReader reader = new org.netpreserve.jwarc.WarcReader(new ByteArrayInputStream(test.getBytes(StandardCharsets.US_ASCII)))) {
            org.netpreserve.jwarc.WarcRecord record = reader.next().get();
            System.out.println(record.headers().sole("Folded").get());
            System.out.println(record.headers().sole("Encoded").get());
        }

    }

}
