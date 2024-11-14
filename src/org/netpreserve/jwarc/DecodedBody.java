/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2024 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;


/**
 * A message body which decodes content on-the-fly using the specified encoding.
 */
public class DecodedBody extends MessageBody {

    public static enum Encoding {
        DEFLATE,
        GZIP,
        BROTLI
    }

    private final ReadableByteChannel channel;
    private final Encoding encoding;
    long position = 0;

    private DecodedBody(ReadableByteChannel channel, Encoding encoding) throws IOException {
        this.encoding = encoding;
        switch (this.encoding) {
        case DEFLATE:
            this.channel = IOUtils.inflateChannel(channel);
            break;
        case GZIP:
            this.channel = IOUtils.gunzipChannel(channel);
            break;
        case BROTLI:
            try {
                this.channel = BrotliUtils.brotliChannel(channel);
            } catch (NoClassDefFoundError e) {
                throw new IOException("Brotli decoder not found, please install org.brotli:dec", e);
            }
            break;
        default:
            throw new IOException("Unsupported encoding");
        }
    }

    public static DecodedBody create(ReadableByteChannel channel, Encoding encoding) throws IOException {
        return new DecodedBody(channel, encoding);
    }

    @Override
    public long position() throws IOException {
        return position;
    };

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int n = channel.read(dst);
        if (n > 0) {
            position += n;
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
}
