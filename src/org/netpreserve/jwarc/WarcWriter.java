/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes records to a WARC file.
 * <p>
 * Compression is not yet implemented.
 */
public class WarcWriter {
    private final WritableByteChannel channel;
    private final WarcCompression compression;
    private final ByteBuffer buffer = ByteBuffer.allocate(8192);
    ByteBuffer recordSeperator = ByteBuffer.allocate(2);
    
    private AtomicLong position = new AtomicLong(0);

    public WarcWriter(WritableByteChannel channel, WarcCompression compression) throws IOException {
        if (compression == WarcCompression.GZIP) {
            throw new UnsupportedOperationException("Writing of GZIP WARC files is not currently supported");
        }
        this.channel = channel;
        this.compression = compression;

        recordSeperator.put(new byte[] { '\n','\n' });

        if (channel instanceof SeekableByteChannel) {
            position.set(((SeekableByteChannel) channel).position());
        }
    }

    public synchronized void write(WarcRecord record) throws IOException {
        // TODO: buffer headers, compression
        position.addAndGet(channel.write(ByteBuffer.wrap(record.serializeHeader())));
        MessageBody body = record.body();
        while (body.read(buffer) >= 0) {
            buffer.flip();
            position.addAndGet(channel.write(buffer));
            buffer.compact();
        }
        recordSeperator.rewind();
        position.addAndGet(channel.write(recordSeperator));
                
    }

    /**
     * Returns the byte position the next record will be written to.
     *
     * If the underlying channel is not seekable the returned value will be relative to the position the channel
     * was in when the WarcWriter was created.
     */
    public long position() {
        return position.get();
    }
}
