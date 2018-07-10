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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class Bench {
    public static void main(String[] args) throws IOException {
        String filename = args[0];

        while (true) {
            if (filename.endsWith(".gz")) {
                long start = System.currentTimeMillis();
                long count = 0;
                byte[] buf = new byte[8192];
                try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(new File(filename)), 8192)) {
                    while (true) {
                        int n = gzis.read(buf);
                        if (n < 0) {
                            break;
                        }
                    }
                }
                System.out.println("gzipinpustream in " + (System.currentTimeMillis() - start) + "ms");
            }

            {
                long start = System.currentTimeMillis();
                long count = 0;
                try (ArchiveReader reader = WARCReaderFactory.get(new File(filename))) {
                    for (ArchiveRecord record : reader) {
                        count++;
                    }
                }
                System.out.println("webarchive-commons " + count + " in " + (System.currentTimeMillis() - start) + "ms");
            }

//            {
//                long start = System.currentTimeMillis();
//                long count = 0;
//                try (WarcReader reader = WarcReaderFactory.getReader(new FileInputStream(filename))) {
//                    for (WarcRecord record : reader) {
//                        count++;
//                    }
//                }
//                System.out.println("jwat " + count + " in " + (System.currentTimeMillis() - start) + "ms");
//            }

            {
                long start = System.currentTimeMillis();
                long count = 0;
                try (WarcReader reader = WarcReaderFactory.getReader(new FileInputStream(filename), 8192)) {
                    for (WarcRecord record : reader) {
                        count++;
                    }
                }
                System.out.println("jwat buff " + count + " in " + (System.currentTimeMillis() - start) + "ms");
            }

            {
                long start = System.currentTimeMillis();
                long count = 0;
                try (org.netpreserve.jwarc.WarcReader reader = new org.netpreserve.jwarc.WarcReader(FileChannel.open(Paths.get(filename)))) {
                    for (org.netpreserve.jwarc.WarcRecord record : reader) {
                        count++;
                    }
                }
                System.out.println("jwarc " + count + " in " + (System.currentTimeMillis() - start) + "ms");
            }

            System.out.println("");

        }
    }
}
