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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

public class Bench {

    @FunctionalInterface
    public interface ThrowingFunction<R, T, E extends Exception> {
        R apply(T t) throws E;
    }

    private static void bench(String name, ThrowingFunction<String, String, IOException> func, String filename) {
        long start = System.currentTimeMillis();
        try {
            String res = func.apply(filename);
            System.out.println(name + " " + res + " in " + (System.currentTimeMillis() - start) + "ms");
        } catch(IOException e) {
            System.out.println(name + " failed after " + (System.currentTimeMillis() - start) + "ms throwing " + e);
        }
    }

    private static String gzip(String filename, int bufferSize) throws IOException {
        byte[] buf = new byte[bufferSize];
        try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(new File(filename)), bufferSize)) {
            while (true) {
                int n = gzis.read(buf);
                if (n < 0) {
                    break;
                }
            }
        }
        return "";
    }

    private static String gzip8k(String filename) throws IOException {
        return gzip(filename, 8192);
    }

    private static String gzip64k(String filename) throws IOException {
        return gzip(filename, 65536);
    }

    private static String webarchiveCommons(String filename) throws IOException {
        long count = 0;
        try (ArchiveReader reader = WARCReaderFactory.get(new File(filename))) {
            for (ArchiveRecord record : reader) {
                count++;
            }
        }
        return Long.toString(count);
    }

    private static String webarchiveCommonsNoDigest(String filename) throws IOException {
        long count = 0;
        try (ArchiveReader reader = WARCReaderFactory.get(new File(filename))) {
            reader.setDigest(false);
            for (ArchiveRecord record : reader) {
                count++;
            }
        }
        return Long.toString(count);
    }

    private static String jwat(String filename) throws IOException {
        long count = 0;
        try (WarcReader reader = WarcReaderFactory.getReader(new FileInputStream(filename))) {
            for (WarcRecord record : reader) {
                count++;
            }
        }
        return Long.toString(count);
    }

    private static String jwatBuff(String filename) throws IOException {
        long count = 0;
        try (WarcReader reader = WarcReaderFactory.getReader(new FileInputStream(filename), 8192)) {
            for (WarcRecord record : reader) {
                count++;
            }
        }
        return Long.toString(count);
    }

    private static String jwarc(String filename) throws IOException {
        long count = 0;
        try (org.netpreserve.jwarc.WarcReader reader = new org.netpreserve.jwarc.WarcReader(FileChannel.open(Paths.get(filename)))) {
            for (org.netpreserve.jwarc.WarcRecord record : reader) {
                count++;
            }
        }
        return Long.toString(count);
    }

    public static void main(String[] args) {
        String filename = args[0];
        System.out.println("Benchmarking " + filename);

        int iterations = 3;

        try {
            Thread.sleep(1000); // sleep a short time to be able to attach a profiler
        } catch(Exception e) {
        }

        for (int i = 1; i <= iterations; i++) {
            System.out.println("iteration " + i);

            if (filename.endsWith(".gz")) {
                bench("gzipinputstream (buffer 8kB)", Bench::gzip8k, filename);
                bench("gzipinputstream (buffer 64kB)", Bench::gzip64k, filename);
            }

            bench("webarchive-commons", Bench::webarchiveCommons, filename);
            bench("webarchive-commons (no digest check)", Bench::webarchiveCommonsNoDigest, filename);

            //bench("jwat", Bench::jwat, filename);
            bench("jwat buff", Bench::jwatBuff, filename);

            bench("jwarc", Bench::jwarc, filename);

            System.out.println("");
        }
    }
}
