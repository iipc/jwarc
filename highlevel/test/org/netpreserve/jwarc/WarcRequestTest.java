/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;
import org.netpreserve.jwarc.parser.ProtocolVersion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class WarcRequestTest {

    final static String warc = "WARC/1.0\r\n" +
            "WARC-Type: request\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Warcinfo-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "Content-Length: 236\r\n" +
            "WARC-Record-ID: <urn:uuid:4885803b-eebd-4b27-a090-144450c11594>\r\n" +
            "Content-Type: application/http;msgtype=request\r\n" +
            "WARC-Concurrent-To: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "\r\n" +
            "GET /images/logoc.jpg HTTP/1.0\r\n" +
            "User-Agent: Mozilla/5.0 (compatible; heritrix/1.10.0)\r\n" +
            "From: stack@example.org\r\n" +
            "Connection: close\r\n" +
            "Referer: http://www.archive.org/\r\n" +
            "Host: www.archive.org\r\n" +
            "Cookie: PHPSESSID=009d7bb11022f80605aa87e18224d824\r\n\r\n";

    @Test
    public void test() throws IOException {
        WarcRequest request = (WarcRequest) WarcRecord.parse(Channels.newChannel(new ByteArrayInputStream(warc.getBytes(StandardCharsets.US_ASCII))));
        assertEquals(Arrays.asList(URI.create("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0")), request.concurrentTo());
        assertEquals("application/http;msgtype=request", request.body().type());
        assertEquals(ProtocolVersion.WARC_1_0, request.version());
        assertEquals(ProtocolVersion.HTTP_1_0, request.http().version());
        assertEquals("close", request.http().headers().sole("connection"));
    }

}