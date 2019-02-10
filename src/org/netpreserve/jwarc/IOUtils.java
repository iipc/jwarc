/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

class IOUtils {

    /**
     * Transfers as many bytes as possible from src to dst.
     * @return the number of bytes transferred.
     */
    static int transfer(ByteBuffer src, ByteBuffer dst) {
        return transferExactly(src, dst, Math.min(dst.remaining(), dst.remaining()));
    }

    /**
     * Transfers up to limits from src to dst.
     * @return the number of bytes transferred.
     */
    static int transfer(ByteBuffer src, ByteBuffer dst, long limit) {
        return transferExactly(src, dst, (int)Math.min(Math.min(src.remaining(), dst.remaining()), limit));
    }

    private static int transferExactly(ByteBuffer src, ByteBuffer dst, int n) {
        if (src.remaining() > n) {
            int savedLimit = src.limit();
            try {
                src.limit(src.position() + n);
                dst.put(src);
                return n;
            } finally {
                src.limit(savedLimit);
            }
        }
        dst.put(src);
        return n;
    }

    static ReadableByteChannel prefixChannel(ByteBuffer prefix, ReadableByteChannel channel) {
        return new ReadableByteChannel() {
            @Override
            public int read(ByteBuffer byteBuffer) throws IOException {
                int n = 0;
                if (prefix.hasRemaining()) {
                    n += IOUtils.transfer(prefix, byteBuffer);
                }
                if (byteBuffer.hasRemaining()) {
                    n += channel.read(byteBuffer);
                }
                return n;
            }

            @Override
            public boolean isOpen() {
                return channel.isOpen();
            }

            @Override
            public void close() throws IOException {
                channel.close();
            }
        };
    }

    static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        while (true) {
            int n = inputStream.read(buffer);
            if (n < 0) break;
            outputStream.write(buffer, 0, n);
        }
    }
}
