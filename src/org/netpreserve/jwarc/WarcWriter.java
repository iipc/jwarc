/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class WarcWriter {
    private final WritableByteChannel channel;
    private final WarcCompression compression;
    private final ByteBuffer buffer = ByteBuffer.allocate(8192);

    public WarcWriter(WritableByteChannel channel, WarcCompression compression) {
        this.channel = channel;
        this.compression = compression;
    }

    public synchronized void write(WarcRecord record) throws IOException {
        // TODO: buffer headers, compression
        channel.write(ByteBuffer.wrap(record.serializeHeader()));
        MessageBody body = record.body();
        while (body.read(buffer) >= 0) {
            buffer.flip();
            channel.write(buffer);
            buffer.compact();
        }
    }
}
