/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class WarcContinuationTest {

    final static String continuation1 = "WARC/1.0\r\n" +
            "WARC-Type: response\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "WARC-Block-Digest: sha1:2ASS7ZUZY6ND6CCHXETFVJDENAWF7KQ2\r\n" +
            "WARC-Payload-Digest: sha1:CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2\r\n" +
            "WARC-IP-Address: 207.241.233.58\r\n" +
            "WARC-Record-ID: <urn:uuid:39509228-ae2f-11b2-763a-aa4c6ec90bb0>\r\n" +
            "WARC-Segment-Number: 1\r\n" +
            "Content-Type: application/http;msgtype=response\r\n" +
            "Content-Length: 1600\r\n" +
            "\r\n" +
            "HTTP/1.1 200 OK\r\n" +
            "Date: Tue, 19 Sep 2006 17:18:40 GMT\r\n" +
            "Server: Apache/2.0.54 (Ubuntu)\r\n" +
            "Last-Modified: Mon, 16 Jun 2003 22:28:51 GMT\r\n" +
            "ETag: \"3e45-67e-2ed02ec0\"\r\n" +
            "Accept-Ranges: bytes\r\n" +
            "Content-Length: 1662\r\n" +
            "Connection: close\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n" +
            "[first 1360 bytes of image/jpeg binary data here]";

    final static String continuation2 = "WARC/1.0\r\n" +
            "WARC-Type: continuation\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
            "WARC-Block-Digest: sha1:T7HXETFVA92MSS7ZENMFZY6ND6WF7KB7\r\n" +
            "WARC-Record-ID: <urn:uuid:70653950-a77f-b212-e434-7a7c6ec909ef>\r\n" +
            "WARC-Segment-Origin-ID: <urn:uuid:39509228-ae2f-11b2-763a-aa4c6ec90bb0>\r\n" +
            "WARC-Segment-Number: 2\r\n" +
            "WARC-Segment-Total-Length: 1902\r\n" +
            "WARC-Identified-Payload-Type: image/jpeg\r\n" +
            "Content-Length: 302\r\n" +
            "\r\n" +
            "[last 302 bytes of image/jpeg binary data here]";


    @Test
    public void test() throws IOException {
        WarcResponse response = (WarcResponse) WarcRecords.parse(new ByteArrayInputStream(continuation1.getBytes(UTF_8)));
        assertEquals(Optional.of(1L), response.segmentNumber());

        WarcContinuation continuation = (WarcContinuation) WarcRecords.parse(new ByteArrayInputStream(continuation2.getBytes(UTF_8)));
        assertEquals(response.id(), continuation.segmentOriginId());
        assertEquals(Optional.of(2L), continuation.segmentNumber());
        assertEquals(Optional.of(1902L), continuation.segmentTotalLength());
    }

    @Test
    public void builder() {
        URI id = URI.create("urn:uuid:70653950-a77f-b212-e434-7a7c6ec909ef");
        WarcContinuation continuation = new WarcContinuation.Builder()
                .segmentOriginId(id)
                .segmentNumber(3)
                .segmentTotalLength(1024)
                .build();
        assertEquals("continuation", continuation.type());
        assertEquals(id, continuation.segmentOriginId());
        assertEquals(Optional.of(3L), continuation.segmentNumber());
        assertEquals(Optional.of(1024L), continuation.segmentTotalLength());
    }

}