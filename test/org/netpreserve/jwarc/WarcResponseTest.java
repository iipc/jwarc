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
import static org.junit.Assert.assertFalse;

public class WarcResponseTest {
    private static String warc = "WARC/1.1\r\n" +
            "WARC-Type: response\r\n" +
            "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
            "WARC-Warcinfo-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
            "WARC-Date: 2016-09-19T17:20:24Z\r\n" +
            "WARC-Block-Digest: sha1:UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2\r\n" +
            "WARC-Payload-Digest: sha1:CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2\r\n" +
            "WARC-IP-Address: 207.241.233.58\r\n" +
            "WARC-Record-ID: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
            "Content-Type: application/http;msgtype=response\r\n" +
            "WARC-Identified-Payload-Type: image/jpeg\r\n" +
            "Content-Length: 1902\r\n" +
            "\r\n" +
            "HTTP/1.1 200 OK\r\n" +
            "Date: Tue, 19 Sep 2016 17:18:40 GMT\r\n" +
            "Server: Apache/2.0.54 (Ubuntu)\r\n" +
            "Last-Modified: Mon, 16 Jun 2013 22:28:51 GMT\r\n" +
            "ETag: \"3e45-67e-2ed02ec0\"\r\n" +
            "Accept-Ranges: bytes\r\n" +
            "Content-Length: 1662\r\n" +
            "Connection: close\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n" +
            "[image/jpeg binary data here]";

    @Test
    public void test() throws IOException {
        WarcResponse response = (WarcResponse) new WarcReader(new ByteArrayInputStream(warc.getBytes(UTF_8))).next().get();
        assertEquals(200, response.http().status());
        assertEquals("OK", response.http().reason());
        assertEquals("image/jpeg", response.http().body().type());
        assertEquals(Optional.of("image/jpeg"), response.payload().identifiedType());
        assertEquals(Optional.of(InetAddresses.forString("207.241.233.58")), response.ipAddress());
        assertEquals(new Digest("sha1", "UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2"), response.body().digests().get(0));
        assertEquals(new Digest("sha1", "CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2"), response.payload().digests().get(0));
        assertEquals(Optional.of(URI.create("urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39")), response.warcinfoID());
    }

    @Test
    public void builder() throws IOException {
        WarcResponse response = new WarcResponse.Builder()
                .recordId(URI.create("urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39"))
                .blockDigest("sha1", "UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2")
                .payloadDigest("sha1", "CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2")
                .warcinfoId(URI.create("urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39"))
                .identifiedPayloadType("image/jpeg")
                .truncated(TruncationReason.DISCONNECT)
                .build();
        assertEquals(new Digest("sha1", "UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2"), response.body().digests().get(0));
        assertEquals(TruncationReason.DISCONNECT, response.truncated());
    }

    @Test
    public void nullTruncation() {
        WarcResponse response = new WarcResponse.Builder()
                .truncated(TruncationReason.DISCONNECT)
                .truncated(TruncationReason.NOT_TRUNCATED)
                .build();
        assertFalse(response.headers().first("WARC-Truncated").isPresent());
        assertEquals(TruncationReason.NOT_TRUNCATED, response.truncated());
    }
}