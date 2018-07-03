/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class Body {
    protected final Headers headers;
    private final BoundedChannel channel;

    Body(Headers headers, ReadableByteChannel channel, ByteBuffer buffer) {
        this.headers = headers;
        this.channel = new BoundedChannel(channel, buffer, length());
    }

    /**
     * The length in bytes of this content body.
     */
    public long length() {
        return headers.first("Content-Length").map(Long::parseLong).orElse(0L);
    }

    /**
     * The MIME type of this content body.
     * <p>
     * Returns "application/octet-stream" if the Content-Type message header is missing.
     */
    public String type() {
        return headers.first("Content-Type").orElse("application/octet-stream");
    }

    public ReadableByteChannel channel() {
        return channel;
    }

    private static class BoundedChannel implements ReadableByteChannel {
        private final ReadableByteChannel channel;
        private final ByteBuffer buffer;
        private long remaining; // bytes remaining to read from channel (excludes buffer)
        private boolean open = true;

        public BoundedChannel(ReadableByteChannel channel, ByteBuffer buffer, long limit) {
            this.channel = channel;
            this.buffer = buffer;
            remaining = limit;
        }

        @Override
        public int read(ByteBuffer dest) throws IOException {
            if (remaining <= 0) return -1;

            if (buffer.hasRemaining()) {
                int n = Math.min((int) Math.min(dest.remaining(), remaining), buffer.remaining());
                int savedLimit = buffer.limit();
                try {
                    buffer.limit(buffer.position() + n);
                    dest.put(buffer);
                } finally {
                    buffer.limit(savedLimit);
                }
                remaining -= n;
                return n;
            }

            /*
             * FIXME: prevent small reads by scattering into buffer?
             */

            int n = (int) Math.min(dest.remaining(), remaining);
            int savedLimit = dest.limit();
            try {
                dest.limit(dest.position() + n);
                n = channel.read(dest);
            } finally {
                dest.limit(savedLimit);
            }
            if (n < 0) throw new EOFException();
            remaining -= n;
            return n;
        }

        @Override
        public boolean isOpen() {
            return open && channel.isOpen();
        }

        @Override
        public void close() throws IOException {
            open = false;
        }
    }
}
