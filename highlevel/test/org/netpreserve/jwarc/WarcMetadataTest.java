/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class WarcMetadataTest {
    final static String warc = "WARC/1.1\r\n" +
            "WARC-Type: metadata\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2016-09-19T17:20:24Z\r\n" +
            "WARC-Record-ID: <urn:uuid:16da6da0-bcdc-49c3-927e-57494593b943>\r\n" +
            "WARC-Concurrent-To: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "Content-Type: application/warc-fields\r\n" +
            "WARC-Block-Digest: sha1:VXT4AF5BBZVHDYKNC2CSM8TEAWDB6CH8\r\n" +
            "Content-Length: 59\r\n" +
            "\r\n" +
            "via: http://www.archive.org/\r\n" +
            "hopsFromSeed: E\r\n" +
            "fetchTimeMs: 565";

    @Test
    public void test() throws IOException {
        WarcMetadata metadata = (WarcMetadata) new WarcReader(new ByteArrayInputStream(warc.getBytes(UTF_8))).next();
        assertEquals("http://www.archive.org/", metadata.fields().sole("via").get());
    }

    @Test
    public void builder() throws IOException {
        Map<String,List<String>> fields = new HashMap<>();
        fields.put("hello", Arrays.asList("one", "two"));
        WarcMetadata metadata = new WarcMetadata.Builder()
                .fields(fields)
                .build();
        assertEquals("application/warc-fields", metadata.body().type());
        assertEquals("one", metadata.fields().first("hello").get());
        assertEquals(Arrays.asList("one", "two"), metadata.fields().all("hello"));
    }

}