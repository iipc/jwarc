/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.testsuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestSuite {
    interface Predicate {
        boolean test(TestRecord record);
    }

    static List<TestReader> readers = Arrays.asList(new CommonsReader(), new JWatReader());

    public static void main(String args[]) throws IOException {

        run("Trims whitespace?", "fold.warc", r -> r.getHeader("Whitespace").equals("one  two"));
        run("Implements header folding?", "fold.warc", r -> r.getHeader("Fold").equals("a b"));
        run("Trims whitespace on folded lines?", "fold.warc", r -> r.getHeader("Fold-Trim").equals("a b c"));
        run("Accepts UTF-8 in field names?", "utf8field.warc", r -> r.getHeader("☃").equals("snowman"));
        run("Accepts UTF-8 in field values?", "fold.warc", r -> r.getHeader("Snowman").equals("☃"));
        System.out.println(readers.get(1).read("fold.warc").getHeaders());
    }

    public static void run(String desc, String warc, Predicate predicate) {
        System.out.printf("| %-40s |", desc);
        for (TestReader reader : readers) {
            String result;
            try {
                TestRecord record = reader.read(warc);
                result = predicate.test(record) ? "YES" : "NO";
            } catch (Exception e) {
                result = "ERR";
            }
            System.out.printf(" %-3s |", result);
        }
        System.out.println();
    }

    public static InputStream open(String name) {
        return TestSuite.class.getResourceAsStream(name);
    }
    public static void test(TestReader reader, String test, boolean value) {
        System.out.println(reader.getClass().getSimpleName() + " " + test + " " + value);
    }
}
