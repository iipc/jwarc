/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import org.junit.Ignore;
import org.junit.Test;

public class GunzipChannelTest {

    private ByteArrayOutputStream getHelloWorldGzipByteStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);
        gzos.write("Hello world".getBytes(StandardCharsets.US_ASCII));
        gzos.finish();
        return baos;
    }

    @Test
    public void test() throws IOException {
        ByteBuffer inBuffer = ByteBuffer.allocate(1024);
        inBuffer.flip();

        ByteArrayOutputStream baos = getHelloWorldGzipByteStream();

        ReadableByteChannel input = Channels.newChannel(new ByteArrayInputStream(baos.toByteArray()));

        GunzipChannel channel = new GunzipChannel(input, inBuffer);

        ByteBuffer buffer = ByteBuffer.allocate(20);
        channel.read(buffer);
        channel.close();
        buffer.flip();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        assertEquals("Hello world", new String(bytes, StandardCharsets.US_ASCII));

    }

    @Test
    public void testExtraField() throws IOException, URISyntaxException {
        ByteBuffer inBuffer = ByteBuffer.allocate(1024);
        inBuffer.flip();

        URL warcFile = getClass().getClassLoader().getResource("org/netpreserve/jwarc/gzip_extra_sl.warc.gz");
        assertNotNull("WARC file gzip_extra_sl.warc.gz not found", warcFile);
        ReadableByteChannel input = FileChannel.open(Paths.get(warcFile.toURI()));
 
        GunzipChannel channel = new GunzipChannel(input, inBuffer);

        ByteBuffer buffer = ByteBuffer.allocate(20);
        channel.read(buffer);
        buffer.flip();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

		assertTrue("Failed reading WARC file: expected \"WARC/1.0\" as first line",
				new String(bytes).startsWith("WARC/1.0"));

        // consume remaining compressed content to determine the length
        do {
            buffer.clear();
        } while (channel.read(buffer) > -1);
        channel.close();

        // check GunzipChannel position
        long warcFileSize = FileChannel.open(Paths.get(warcFile.toURI())).size();
        assertEquals("Wrong input position", warcFileSize, channel.inputPosition());
    }

    private void checkExternalBuffer(ByteBuffer buffer) throws IOException {
        ByteArrayOutputStream baos = getHelloWorldGzipByteStream();

        ReadableByteChannel input = Channels.newChannel(new ByteArrayInputStream(baos.toByteArray()));

        GunzipChannel channel = new GunzipChannel(input, buffer);
        ByteBuffer output = ByteBuffer.allocate(20);
        int n = channel.read(output);
        channel.close();
        assertEquals(11, n);
        assertEquals("Hello world", new String(output.array(), 0, 11, StandardCharsets.US_ASCII));
    }

    @Test(expected = IllegalArgumentException.class)
    public void externalBufferNoArray() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024).asReadOnlyBuffer();
        buffer.flip();
        checkExternalBuffer(buffer);
    }

    @Ignore("User must ensure buffer is in read state")
    @Test
    public void externalBufferNoReadState() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        // not calling buffer.flip()
        checkExternalBuffer(buffer);
    }

    @Test
    public void externalBufferByteOrderLE() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.flip();
        checkExternalBuffer(buffer);
    }

    @Test
    public void externalBufferByteOrderBE() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.flip();
        checkExternalBuffer(buffer);
    }
}