/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2020 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import org.junit.Test;
import org.netpreserve.jwarc.HttpResponse;
import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcCompression;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcResponse;
import org.netpreserve.jwarc.WarcWriter;

public class WarcWriterTest {

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