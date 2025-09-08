/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import com.github.luben.zstd.ZstdOutputStream;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ZstdDecompressingChannelTest extends TestCase {

    @Test
    public void test() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZstdOutputStream zstd = new ZstdOutputStream(baos);
        zstd.write("hello world".getBytes());
        zstd.flush();
        zstd.close();

        ByteBuffer buffer1 = ByteBuffer.allocate(1024);
        buffer1.flip();
        ZstdDecompressingChannel channel = new ZstdDecompressingChannel(LengthedBody.create(baos.toByteArray()), buffer1);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
    }

}