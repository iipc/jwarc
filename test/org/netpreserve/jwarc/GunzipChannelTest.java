/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;
import org.netpreserve.jwarc.GunzipChannel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GunzipChannelTest {

    @Test
    public void test() throws IOException {
        ByteBuffer inBuffer = ByteBuffer.allocate(1024);
        inBuffer.flip();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);
        gzos.write("Hello world".getBytes());
        gzos.finish();

        ReadableByteChannel input = Channels.newChannel(new ByteArrayInputStream(baos.toByteArray()));

        GunzipChannel channel = new GunzipChannel(input, inBuffer);

        ByteBuffer buffer = ByteBuffer.allocate(20);
        channel.read(buffer);
        buffer.flip();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        assertEquals("Hello world", new String(bytes));

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
    }

}