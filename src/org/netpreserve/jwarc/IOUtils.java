/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018-2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import javax.net.ssl.SSLSocketFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class is public only due to technical constraints. Please don't depend on it your own code.
 */
public final class IOUtils {

    /**
     * Transfers as many bytes as possible from src to dst.
     *
     * @return the number of bytes transferred.
     */
    static int transfer(ByteBuffer src, ByteBuffer dst) {
        return transferExactly(src, dst, Math.min(src.remaining(), dst.remaining()));
    }

    /**
     * Transfers up to limits from src to dst.
     *
     * @return the number of bytes transferred.
     */
    static int transfer(ByteBuffer src, ByteBuffer dst, long limit) {
        return transferExactly(src, dst, (int) Math.min(Math.min(src.remaining(), dst.remaining()), limit));
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

    static int transfer(ReadableByteChannel src, ByteBuffer dst, long limit) throws IOException {
        if (dst.remaining() > limit) {
            int savedLimit = dst.limit();
            try {
                dst.limit(dst.position() + (int) limit);
                int n = src.read(dst);
                return n;
            } finally {
                dst.limit(savedLimit);
            }
        }
        return src.read(dst);
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

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        while (true) {
            int n = inputStream.read(buffer);
            if (n < 0) break;
            outputStream.write(buffer, 0, n);
        }
    }

    public static ReadableByteChannel gunzipChannel(ReadableByteChannel gzipped) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        return new GunzipChannel(gzipped, buffer);
    }

    public static ReadableByteChannel inflateChannel(ReadableByteChannel deflated) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        return new InflateChannel(deflated, buffer);
    }

    static Socket connect(String scheme, String host, int port) throws IOException {
        Objects.requireNonNull(host);
        if ("http".equalsIgnoreCase(scheme)) {
            return new Socket(host, port < 0 ? 80 : port);
        } else if ("https".equalsIgnoreCase(scheme)) {
            return SSLSocketFactory.getDefault().createSocket(host, port < 0 ? 443 : port);
        } else {
            throw new IllegalArgumentException("Unsupported URI scheme: " + scheme);
        }
    }

    public static byte[] readNBytes(InputStream stream, int n) throws IOException {
        byte[] buffer = new byte[n];
        for (int remaining = n; remaining > 0; ) {
            int read = stream.read(buffer, buffer.length - remaining, remaining);
            if (read < 0) {
                return Arrays.copyOf(buffer, buffer.length - remaining);
            }
            remaining -= read;
        }
        return buffer;
    }

    /**
     * Ensures that at least the specified number of bytes are available in the buffer by reading from the provided channel
     * if necessary. If the buffer already contains enough bytes, no data is read. If the channel reaches EOF before the
     * required number of bytes is available, an EOFException is thrown.
     *
     * @return the total number of bytes read from the channel to ensure the required availability in the buffer.
     * @throws IOException if an I/O error occurs while reading from the channel.
     * @throws EOFException if the channel reaches EOF before the required number of bytes are available.
     */
    static int ensureAvailable(ReadableByteChannel channel, ByteBuffer buffer, int needed) throws IOException {
        int totalRead = 0;
        if (buffer.remaining() >= needed) return 0;
        buffer.compact();
        while (buffer.position() < needed) {
            int n = channel.read(buffer);
            if (n == -1) throw new EOFException("expected " + (needed - totalRead) + " more bytes in channel");
            totalRead += n;
        }
        buffer.flip();
        return totalRead;
    }
}
