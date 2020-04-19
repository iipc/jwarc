/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Assert;
import org.junit.Test;
import org.netpreserve.jwarc.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

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
            "Content-Length: 277\r\n" +
            "\r\n" +
            "HTTP/1.1 200 OK\r\n" +
            "Date: Tue, 19 Sep 2016 17:18:40 GMT\r\n" +
            "Server: Apache/2.0.54 (Ubuntu)\r\n" +
            "Last-Modified: Mon, 16 Jun 2013 22:28:51 GMT\r\n" +
            "ETag: \"3e45-67e-2ed02ec0\"\r\n" +
            "Accept-Ranges: bytes\r\n" +
            "Content-Length: 29\r\n" +
            "Connection: close\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n" +
            "[image/jpeg binary data here]";

    @Test
    public void test() throws IOException {
        WarcResponse response = sampleResponse();
        assertEquals(200, response.http().status());
        assertEquals("OK", response.http().reason());
        Assert.assertEquals(MediaType.parse("image/jpeg"), response.http().contentType());
        assertEquals(Optional.of(MediaType.parse("image/jpeg")), response.identifiedPayloadType());
        assertEquals(Optional.of(InetAddress.getByAddress(new byte[]{(byte) 207, (byte) 241, (byte) 233, 58})), response.ipAddress());
        assertEquals(Optional.of(new WarcDigest("sha1", "UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2")), response.blockDigest());
        assertEquals(Optional.of(new WarcDigest("sha1", "CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2")), response.payloadDigest());
        assertEquals(Optional.of(URI.create("urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39")), response.warcinfoID());
    }

    private WarcResponse sampleResponse() throws IOException {
        return sampleResponse(warc, false);
    }

    private WarcResponse sampleResponse(String warc, boolean gzip) throws IOException {
        return sampleResponse(warc.getBytes(UTF_8), gzip);
    }

    private WarcResponse sampleResponse(byte[] warc, boolean gzip) throws IOException {
        if (gzip) {
            try (ByteArrayOutputStream bs = new ByteArrayOutputStream(warc.length); GZIPOutputStream gz = new GZIPOutputStream(bs)) {
                gz.write(warc);
                gz.close();
                warc = bs.toByteArray();
            }
        }
        return (WarcResponse) new WarcReader(new ByteArrayInputStream(warc)).next().get();
    }

    @Test
    public void builder() throws IOException {
        WarcResponse response = new WarcResponse.Builder(URI.create("http://example.org/"))
                .recordId(URI.create("urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39"))
                .blockDigest("sha1", "UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2")
                .payloadDigest("sha1", "CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2")
                .warcinfoId(URI.create("urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39"))
                .identifiedPayloadType("image/jpeg")
                .truncated(WarcTruncationReason.DISCONNECT)
                .build();
        assertEquals(Optional.of(new WarcDigest("sha1", "UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2")), response.blockDigest());
        assertEquals(WarcTruncationReason.DISCONNECT, response.truncated());
    }

    @Test
    public void nullTruncation() {
        WarcResponse response = new WarcResponse.Builder(URI.create("http://example.org/"))
                .truncated(WarcTruncationReason.DISCONNECT)
                .truncated(WarcTruncationReason.NOT_TRUNCATED)
                .build();
        assertFalse(response.headers().first("WARC-Truncated").isPresent());
        assertEquals(WarcTruncationReason.NOT_TRUNCATED, response.truncated());
    }

    @Test
    public void callingHttpShouldNotCorruptBody() throws IOException {
        WarcResponse response = sampleResponse();
        HttpResponse http = response.http();
        assertEquals(0, response.body().position());
        assertEquals('H', response.body().stream().read());
        assertEquals(0, http.body().position());
        assertEquals('[', http.body().stream().read());

    }

    @Test(expected = IllegalStateException.class)
    public void readingBodyShouldInvalidateHttp() throws IOException {
        WarcResponse response = sampleResponse();
        response.body().read(ByteBuffer.allocate(1));
        response.http();
    }

    @Test
    public void testNoHttpContentLength() throws IOException {
        String w = warc.replace("Content-Length: 29\r\n", "").replace("Content-Length: 277", "Content-Length: 257");
        // test for uncompressed and compressed WARC record
        WarcResponse[] responses = { sampleResponse(w, false), sampleResponse(w, true) };
        for (WarcResponse response : responses) {
            HttpResponse http = response.http();
            Optional<String> contentLengthHeader = http.headers().first("content-length");
            assertFalse("Test setup: HTTP Content-Length header not removed", contentLengthHeader.isPresent());
            Optional<WarcPayload> payload = response.payload();
            MessageBody payloadBody = payload.get().body();
            assertNotEquals(payloadBody.size(), 0);
            ByteBuffer buf = ByteBuffer.allocate(1024);
            payloadBody.read(buf);
            buf.flip();
            assertEquals("[image/jpeg binary data here]", new String(buf.array(), 0, buf.limit(), UTF_8));
        }
    }
}