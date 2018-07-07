/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class WarcinfoTest {
    final static String warc = "WARC/1.0\r\n" +
            "WARC-Type: warcinfo\r\n" +
            "WARC-Date: 2006-09-19T17:20:14Z\r\n" +
            "WARC-Record-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
            "WARC-Filename:hello.warc\r\n" +
            "Content-Type: application/warc-fields\r\n" +
            "Content-Length: 399\r\n" +
            "\r\n" +
            "software: Heritrix 1.12.0 http://crawler.archive.org\r\n" +
            "hostname: crawling017.archive.org\r\n" +
            "ip: 207.241.227.234\r\n" +
            "isPartOf: testcrawl-20050708\r\n" +
            "description: testcrawl with WARC output\r\n" +
            "operator: IA\\_Admin\r\n" +
            "http-header-user-agent:\r\n" +
            " Mozilla/5.0 (compatible; heritrix/1.4.0 +http://crawler.archive.org)\r\n" +
            "format: WARC file version 1.0\r\n" +
            "conformsTo:\r\n" +
            " http://www.archive.org/documents/WarcFileFormat-1.0.html\r\n\r\n";

    @Test
    public void test() throws IOException {
        Warcinfo warcinfo = (Warcinfo) WarcRecord.parse(Channels.newChannel(new ByteArrayInputStream(warc.getBytes(UTF_8))));
        assertEquals(URI.create("urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39"), warcinfo.id());
        assertEquals(Instant.parse("2006-09-19T17:20:14Z"), warcinfo.date());
        assertEquals("hello.warc", warcinfo.filename().get());
        assertEquals(399, warcinfo.body().size());
        assertEquals("application/warc-fields", warcinfo.body().type());
        Headers fields = warcinfo.fields();
        assertEquals("207.241.227.234", fields.sole("ip").get());
        assertEquals("http://www.archive.org/documents/WarcFileFormat-1.0.html", fields.sole("conformsTo").get());
    }

    @Test
    public void builder() throws IOException {
        Map<String,List<String>> fields = new HashMap<>();
        fields.put("hello", Arrays.asList("one", "two"));
        Warcinfo warcinfo = new Warcinfo.Builder()
                .filename("hello.warc")
                .fields(fields)
                .build();
        assertEquals("warcinfo", warcinfo.type());
        assertEquals("hello.warc", warcinfo.filename().get());
        assertEquals("one", warcinfo.fields().first("hello").get());
        assertEquals(Arrays.asList("one", "two"), warcinfo.fields().all("hello"));
        assertEquals("application/warc-fields", warcinfo.body().type());
    }

}