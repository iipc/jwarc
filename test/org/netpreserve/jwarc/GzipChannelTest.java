/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2020 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

public class GzipChannelTest {

    protected String text = "Hello world";
    protected byte[] textBytes = text.getBytes(StandardCharsets.US_ASCII);

    private void checkGzip(byte[] gzipped) {
        // did we get valid gzipped data?
        short magic = ByteBuffer.wrap(gzipped).order(ByteOrder.LITTLE_ENDIAN).getShort();
        assertEquals(magic, GzipChannel.GZIP_MAGIC);
        assertTrue(gzipped.length >= 20);
    }

    @Test
    public void test() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GzipChannel channel = new GzipChannel(Channels.newChannel(baos));
        channel.write(ByteBuffer.wrap(textBytes));
        channel.close();
        byte[] gzipped = baos.toByteArray();

        checkGzip(gzipped);

        GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        byte[] inBytes = new byte[8192];
        int n = gzis.read(inBytes);

        assertEquals(n, textBytes.length);
        assertEquals(text, new String(inBytes, 0, n, StandardCharsets.US_ASCII));
    }

    /**
     * Test that zero content (empty string, zero bytes input) is written as valid
     * gzip data, otherwise uncompressing will cause an error.
     */
    @Test
    public void testEmpty() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GzipChannel channel = new GzipChannel(Channels.newChannel(baos));
        channel.write(ByteBuffer.allocate(0));
        channel.finish();
        channel.close();
        byte[] gzipped = baos.toByteArray();

        checkGzip(gzipped);

        byte[] inBytes = new byte[8192];
        int n = (new GZIPInputStream(new ByteArrayInputStream(gzipped))).read(inBytes);
        assertTrue(n <= 0);

        // test without calling write() and finish()
        baos = new ByteArrayOutputStream();
        channel = new GzipChannel(Channels.newChannel(baos));
        channel.close();
        gzipped = baos.toByteArray();

        checkGzip(gzipped);

        n = (new GZIPInputStream(new ByteArrayInputStream(gzipped))).read(inBytes);
        assertTrue(n <= 0);
    }

    @Test
    public void testMultiMember() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GzipChannel channel = new GzipChannel(Channels.newChannel(baos));
        int posSecond = 0;
        posSecond += channel.write(ByteBuffer.wrap(textBytes));
        posSecond += channel.finish(); // finish first member
        channel.write(ByteBuffer.wrap(textBytes));
        channel.close();
        byte[] gzipped = baos.toByteArray();

        checkGzip(gzipped);
        checkGzip(Arrays.copyOfRange(gzipped, posSecond, gzipped.length));

        GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        byte[] inBytes = new byte[8192];
        int n = gzis.read(inBytes);

        assertEquals(n, textBytes.length);
        assertEquals(text, new String(inBytes, 0, n, StandardCharsets.US_ASCII));

        // read second member
        assertTrue(gzis.available() > 0);
        n = gzis.read(inBytes);
        assertEquals(n, textBytes.length);
        assertEquals(text, new String(inBytes, 0, n, StandardCharsets.US_ASCII));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBufferNoArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GzipChannel channel = new GzipChannel(Channels.newChannel(baos), ByteBuffer.allocate(1024).asReadOnlyBuffer());
        channel.close();
    }
}