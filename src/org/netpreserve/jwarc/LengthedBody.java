/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * A message body with a known length.
 */
public class LengthedBody extends MessageBody {
    static final LengthedBody EMPTY = LengthedBody.create(Channels.newChannel(new ByteArrayInputStream(new byte[0])),
            ByteBuffer.allocate(0), 0);
    private final ReadableByteChannel channel;
    final ByteBuffer buffer;
    private final long size;
    long position = 0;
    private boolean open = true;
    ByteBuffer pushback;

    private LengthedBody(ReadableByteChannel channel, ByteBuffer buffer, long size) {
        this.channel = channel;
        this.buffer = buffer;
        this.size = size;
    }

    public static LengthedBody create(ReadableByteChannel channel, ByteBuffer buffer, long size) {
        if (channel instanceof SeekableByteChannel) {
            return new Seekable((SeekableByteChannel) channel, buffer, size);
        }
        return new LengthedBody(channel, buffer, size);
    }

    static LengthedBody createFromContentLength(ReadableByteChannel channel, ByteBuffer buffer, Long contentLengthHeader) throws IOException {
        long length;
        if (channel instanceof LengthedBody.LengthedReadableByteChannel) {
            LengthedBody.LengthedReadableByteChannel lengthed = (LengthedBody.LengthedReadableByteChannel) channel;
            length = lengthed.size() - lengthed.position() + buffer.remaining();
        } else if (channel instanceof LengthedBody) {
            LengthedBody lengthed = (LengthedBody) channel;
            length = lengthed.size() - lengthed.position() + buffer.remaining();
        } else if (channel instanceof SeekableByteChannel) {
            SeekableByteChannel seekable = (SeekableByteChannel) channel;
            length = seekable.size() - seekable.position() + buffer.remaining();
        } else {
            if (contentLengthHeader == null) throw new IllegalArgumentException("unable to determine length");
            length = contentLengthHeader;
        }
        return new LengthedBody(channel, buffer, length);
    }

    synchronized void pushback(byte[] pushback) {
        if (pushback.length > position) throw new IllegalArgumentException("pushback would result in negative position");
        if (this.pushback != null) throw new IllegalStateException("already pushed back");
        this.pushback = ByteBuffer.wrap(pushback);
        position -= pushback.length;
    }

    @Override
    public synchronized int read(ByteBuffer dest) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        if (position >= size) {
            return -1;
        }

        if (pushback != null) {
            int n = IOUtils.transfer(pushback, dest, size - position);
            if (!pushback.hasRemaining()) {
                pushback = null;
            }
            position += n;
            return n;
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

    public synchronized void consume() throws IOException {
        discardPushback();
        while (true) {
            // if remaining body is in the buffer we only need to advance the buffer position
            long remaining = size - position;
            if (remaining <= buffer.remaining()) {
                buffer.position(buffer.position() + (int) remaining);
                position = size;
                break;
            }

            // if underlying channel is seekable discard buffer and try to skip directly
            if (channel instanceof SeekableByteChannel) {
                try {
                    SeekableByteChannel seekable = (SeekableByteChannel) channel;
                    seekable.position(seekable.position() + remaining - buffer.remaining());
                    position = size;
                    buffer.position(buffer.limit());
                    break;
                } catch (IOException e) {
                    // alas!
                }
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

    void discardPushback() {
        if (pushback != null) {
            position += pushback.remaining();
            pushback = null;
        }
    }

    public interface LengthedReadableByteChannel extends ReadableByteChannel {
        public long position();
        public long size();
    }

    ReadableByteChannel discardPushbackOnRead() {
        return new LengthedReadableByteChannel() {
            @Override
            public int read(ByteBuffer byteBuffer) throws IOException {
                discardPushback();
                return LengthedBody.this.read(byteBuffer);
            }

            @Override
            public boolean isOpen() {
                return LengthedBody.this.isOpen();
            }

            @Override
            public void close() throws IOException {
                LengthedBody.this.close();
            }

            @Override
            public long position() {
                return LengthedBody.this.position;
            }

            @Override
            public long size() {
                return LengthedBody.this.size;
            }
        };
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
        return Channels.newInputStream(this);
    }

    private static class Seekable extends LengthedBody implements SeekableByteChannel {
        private final SeekableByteChannel seekable;

        Seekable(SeekableByteChannel channel, ByteBuffer buffer, long size) {
            super(channel, buffer, size);
            this.seekable = channel;
        }

        @Override
        public synchronized SeekableByteChannel position(long position) throws IOException {
            if (position < 0) throw new IllegalArgumentException("negative position");
            long relative = Math.min(size(), position) - this.position;
            if (relative >= 0 && relative < buffer.remaining() && pushback == null) {
                buffer.position((int) (buffer.position() + relative));
            } else {
                buffer.position(buffer.limit());
                seekable.position(seekable.position() + relative);
                pushback = null;
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

        @Override
        SeekableByteChannel discardPushbackOnRead() {
            return new SeekableByteChannel() {
                @Override
                public int read(ByteBuffer byteBuffer) throws IOException {
                    discardPushback();
                    return Seekable.this.read(byteBuffer);
                }

                @Override
                public int write(ByteBuffer byteBuffer) throws IOException {
                    throw new NonWritableChannelException();
                }

                @Override
                public long position() throws IOException {
                    discardPushback();
                    return Seekable.this.position();
                }

                @Override
                public SeekableByteChannel position(long l) throws IOException {
                    discardPushback();
                    return Seekable.this.position(l);
                }

                @Override
                public long size() throws IOException {
                    return Seekable.this.size();
                }

                @Override
                public SeekableByteChannel truncate(long l) throws IOException {
                    throw new NonWritableChannelException();
                }

                @Override
                public boolean isOpen() {
                    return Seekable.this.isOpen();
                }

                @Override
                public void close() throws IOException {
                    Seekable.this.close();
                }
            };
        }
    }

}
