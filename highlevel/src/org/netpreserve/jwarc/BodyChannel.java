/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

public class BodyChannel implements ReadableByteChannel {
    protected final Headers headers;
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private final long size;
    private long position = 0;
    private boolean open = true;

    BodyChannel(Headers headers, ReadableByteChannel channel, ByteBuffer buffer) {
        this.headers = headers;
        this.channel = channel;
        this.buffer = buffer;
        this.size = headers.sole("Content-Length").map(Long::parseLong).orElse(0L);
    }

    @Override
    public int read(ByteBuffer dest) throws IOException {
        if (position >= size) {
            return -1;
        }

        int bytesToRead = (int) Math.min(dest.remaining(), size - position);
        if (buffer.hasRemaining()) {
            if (buffer.remaining() < bytesToRead) {
                bytesToRead = buffer.remaining();
            }
            int savedLimit = buffer.limit();
            try {
                buffer.limit(buffer.position() + bytesToRead);
                dest.put(buffer);
            } finally {
                buffer.limit(savedLimit);
            }
            position += bytesToRead;
            return bytesToRead;
        }

        /*
         * FIXME: prevent small reads by scattering into our buffer?
         */

        int savedLimit = dest.limit();
        try {
            dest.limit(dest.position() + bytesToRead);
            int actuallyRead = channel.read(dest);
            if (actuallyRead < 0) {
                throw new EOFException();
            }
            position += actuallyRead;
            return actuallyRead;
        } finally {
            dest.limit(savedLimit);
        }
    }

    void consume() throws IOException {
        while (true) {
            // if remaining body is in the buffer we only need to advance the buffer position
            long remaining = size - position;
            if (remaining < buffer.remaining()) {
                buffer.position(buffer.position() + (int) remaining);
                position = size;
                break;
            }

            // if underlying channel is seekable discard buffer and skip directly
            if (channel instanceof SeekableByteChannel) {
                SeekableByteChannel seekable = (SeekableByteChannel)channel;
                seekable.position(seekable.position() + remaining - buffer.remaining());
                position = size;
                buffer.position(buffer.limit());
                break;
            }

            // otherwise move forward by discarding and refilling buffer
            position += buffer.remaining();
            buffer.clear();
            if (channel.read(buffer) < 0) {
                throw new EOFException();
            }
            buffer.flip();
        }
    }

    @Override
    public boolean isOpen() {
        return open && channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        open = false;
    }

    /**
     * The length in bytes of the content body.
     */
    public long size() {
        return size;
    }

    /**
     * The current position in bytes within the content body.
     */
    public long position() {
        return position;
    }

    /**
     * The content type of the body.
     * <p>
     * Returns "application/octet-stream" if the Content-Type header is missing.
     */
    public String type() {
        return headers.sole("Content-Type").orElse("application/octet-stream");
    }

    /**
     * Returns an InputStream for reading this body.
     */
    public InputStream stream() {
        return new Stream();
    }

    private class Stream extends InputStream {
        @Override
        public int read(byte[] b) throws IOException {
            return BodyChannel.this.read(ByteBuffer.wrap(b));
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return BodyChannel.this.read(ByteBuffer.wrap(b, off, len));
        }

        @Override
        public int available() {
            return (int) Math.min(position - size, Integer.MAX_VALUE);
        }

        @Override
        public void close() throws IOException {
            BodyChannel.this.close();
        }

        @Override
        public int read() throws IOException {
            if (position >= size) {
                return -1;
            }
            while (!buffer.hasRemaining()) {
                buffer.compact();
                if (channel.read(buffer) < 0) {
                    throw new EOFException();
                }
                buffer.flip();
            }
            position++;
            return buffer.get();
        }
    }
}
