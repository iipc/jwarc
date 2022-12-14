/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2020 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import com.sun.net.httpserver.HttpServer;
import org.junit.Test;
import org.netpreserve.jwarc.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WarcWriterTest {

    @Test
    public void fetch() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        byte[] body = "Hello world!".getBytes(StandardCharsets.UTF_8);
        MessageDigest bodyDigest = MessageDigest.getInstance("SHA-1");
        bodyDigest.update(body);

        // get loopback address
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Test-Header", "present");
            exchange.sendResponseHeaders(256, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            WarcWriter warcWriter = new WarcWriter(Channels.newChannel(out));
            URI uri = new URI("http", null, server.getAddress().getHostString(),
                    server.getAddress().getPort(), "/", null, null);
            warcWriter.fetch(uri);

            WarcReader warcReader = new WarcReader(new ByteArrayInputStream(out.toByteArray()));
            warcReader.calculateBlockDigest();

            WarcResponse response = (WarcResponse) warcReader.next()
                    .orElseThrow(() -> new RuntimeException("Missing response record"));
            assertEquals(256, response.http().status());
            assertEquals("present", response.http().headers().first("Test-Header").orElse(null));
            assertTrue(response.blockDigest().isPresent());
            assertEquals(response.calculatedBlockDigest(), response.blockDigest());
            assertEquals(new WarcDigest(bodyDigest).toString(), response.payloadDigest().map(Object::toString).orElse(null));

            WarcRequest request = (WarcRequest) warcReader.next()
                    .orElseThrow(() -> new RuntimeException("Missing request record"));
            assertTrue(request.blockDigest().isPresent());
            assertEquals(request.calculatedBlockDigest(), request.blockDigest());
        } finally {
            server.stop(0);
        }
    }


    @Test
    public void gzippedWarc() throws IOException, URISyntaxException {
        // write gzipped WARC file to memory
        String body = "<html><head/><body>Test</body></html>";
        byte[] bodyBytes = body.getBytes(StandardCharsets.US_ASCII);
        HttpResponse response = new HttpResponse.Builder(200, "OK").addHeader("Server", "test")
                .body(MediaType.HTML, bodyBytes).build();
        WarcResponse record = new WarcResponse.Builder(URI.create("https://example.org/")).body(response).build();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel output = Channels.newChannel(baos);
        WarcWriter writer = new WarcWriter(output, WarcCompression.GZIP);
        writer.write(record);
        writer.close();
        byte[] warcBytes = baos.toByteArray();

        // did we get a gzipped WARC?
        assertEquals(warcBytes[0], (byte) 0x1f);
        assertEquals(warcBytes[1], (byte) 0x8b);

        // does position indicate the end of the gzipped WARC record?
        assertEquals(warcBytes.length, writer.position());

        // uncompress and check whether body is contained
        ByteArrayInputStream bais = new ByteArrayInputStream(warcBytes);
        GZIPInputStream gzin = new GZIPInputStream(bais);
        byte[] warcb = new byte[8192];
        int n = gzin.read(warcb);
        String warc = new String(warcb, 0, n, StandardCharsets.US_ASCII);
        assertTrue(warc.contains(body));

        // read gzipped WARC using WarcReader
        bais.reset();
        WarcReader reader = new WarcReader(bais);
        Optional<WarcRecord> res = reader.next();
        reader.close();
        assertTrue(res.isPresent());
        WarcRecord rec = res.get();
        assertTrue(rec instanceof WarcResponse);
        WarcResponse resp = (WarcResponse) rec;
        ByteBuffer buf = ByteBuffer.allocate(64);
        n = resp.http().body().read(buf);
        buf.flip();
        byte[] b = new byte[n];
        buf.get(b);
        assertEquals(body, new String(b, StandardCharsets.US_ASCII));
        assertTrue(resp.http().headers().map().containsKey("Server"));
        assertEquals("test", resp.http().headers().map().get("Server").get(0));
    }

}