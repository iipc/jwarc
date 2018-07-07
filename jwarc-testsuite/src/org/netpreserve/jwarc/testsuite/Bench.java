/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.testsuite;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Bench {
    public static void main(String[] args) throws IOException {
        jwat();
        return;
    }

    private static void commons() throws IOException {
        while (true) {
            long start = System.currentTimeMillis();
            try (ArchiveReader reader = WARCReaderFactory.get(new File("/tmp/her.warc"))) {
                for (ArchiveRecord record: reader) {
                    if (record == null) break;
//                    System.out.println(record + " " + record.body().length());
                }
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    private static void jwat() throws IOException {
        while (true) {
            long start = System.currentTimeMillis();
            try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get("/tmp/her.warc")), 8192)) {
                WarcReader reader = WarcReaderFactory.getReaderUncompressed(in);
                while (true) {
                    WarcRecord record = reader.getNextRecord();
                    if (record == null) break;
//                    System.out.println(record + " " + record.body().length());
                }
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }
}
