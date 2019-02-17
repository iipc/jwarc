/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

/**
 * A message body with a known length.
 */
class LengthedBody extends MessageBody {
    private final ReadableByteChannel channel;
    final ByteBuffer buffer;
    private final long size;
    long position = 0;
    private boolean open = true;

    private LengthedBody(ReadableByteChannel channel, ByteBuffer buffer, long size) {
        this.channel = channel;
        this.buffer = buffer;
        this.size = size;
    }

    static LengthedBody create(ReadableByteChannel channel, ByteBuffer buffer, long size) {
        if (channel instanceof SeekableByteChannel) {
            return new Seekable((SeekableByteChannel) channel, buffer, size);
        }
        return new LengthedBody(channel, buffer, size);
    }

    @Override
    public int read(ByteBuffer dest) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        if (position >= size) {
            return -1;
        }

        if (buffer.hasRemaining()) {
            int n = IOUtils.transfer(buffer, dest, size - position);
            position += n;
            return n;
        }

        /*
         * FIXME: prevent small reads by scattering into our buffer?
         */

        int savedLimit = dest.limit();
        try {
            dest.limit(dest.position() + (int) Math.min(dest.remaining(), size - position));
            int actuallyRead = channel.read(dest);
            if (actuallyRead < 0) {
                throw new EOFException("expected " + (size - position) + " more bytes in file");
            }
            position += actuallyRead;
            return actuallyRead;
        } finally {
            dest.limit(savedLimit);
        }
    }

    public void consume() throws IOException {
        while (true) {
            // if remaining body is in the buffer we only need to advance the buffer position
            long remaining = size - position;
            if (remaining <= buffer.remaining()) {
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
    @Override
    public long size() {
        return size;
    }

    /**
     * The current position in bytes within the content body.
     */
    @Override
    public long position() {
        return position;
    }

    /**
     * Returns an InputStream for reading this body.
     */
    @Override
    public InputStream stream() {
        return new Stream();
    }

    private class Stream extends InputStream {
        @Override
        public int read(byte[] b) throws IOException {
            return LengthedBody.this.read(ByteBuffer.wrap(b));
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return LengthedBody.this.read(ByteBuffer.wrap(b, off, len));
        }

        @Override
        public int available() {
            return (int) Math.min(position - size, Integer.MAX_VALUE);
        }

        @Override
        public void close() throws IOException {
            LengthedBody.this.close();
        }

        @Override
        public int read() throws IOException {
            if (!open) {
                throw new ClosedChannelException();
            }
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

    private static class Seekable extends LengthedBody implements SeekableByteChannel {
        private final SeekableByteChannel seekable;

        Seekable(SeekableByteChannel channel, ByteBuffer buffer, long size) {
            super(channel, buffer, size);
            this.seekable = channel;
        }

        @Override
        public SeekableByteChannel position(long position) throws IOException {
            if (position < 0) throw new IllegalArgumentException("negative position");
            long relative = Math.min(size(), position) - this.position;
            if (relative >= 0 && relative < buffer.remaining()) {
                buffer.position((int) (buffer.position() + relative));
            } else {
                buffer.position(buffer.limit());
                seekable.position(seekable.position() + relative);
            }
            this.position += relative;
            return this;
        }

        @Override
        public int write(ByteBuffer byteBuffer) throws IOException {
            throw new NonWritableChannelException();
        }

        @Override
        public SeekableByteChannel truncate(long l) throws IOException {
            throw new NonWritableChannelException();
        }
    }

}
