/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2020 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * A ReadableByteChannel inflating deflate-compressed content on read. Used to
 * uncompress HTTP payload with header <code>Content-Encoding: deflate</code>.
 */
public class InflateChannel implements ReadableByteChannel {

    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private final Inflater inflater = new Inflater(true);

    public InflateChannel(ReadableByteChannel channel, ByteBuffer buffer) throws IllegalArgumentException {
        this.channel = channel;
        this.buffer = buffer;
        if (!buffer.hasArray()) {
            throw new IllegalArgumentException("ByteBuffer must be array-backed and writable");
        }
    }

    @Override
    public int read(ByteBuffer dest) throws IOException {
        if (inflater.finished()) {
            return -1;
        }

        if (inflater.needsInput()) {
            if (!buffer.hasRemaining()) {
                buffer.compact();
                channel.read(buffer);
                buffer.flip();
            }
            inflater.setInput(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        }

        try {
            int n = inflater.inflate(dest.array(), dest.arrayOffset() + dest.position(), dest.remaining());
            dest.position(dest.position() + n);

            int newBufferPosition = buffer.limit() - inflater.getRemaining();
            buffer.position(newBufferPosition);

            return n;
        } catch (DataFormatException e) {
            throw new ZipException(e.getMessage());
        }
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
