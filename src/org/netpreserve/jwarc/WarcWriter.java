/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicLong;

public class WarcWriter {
    private final WritableByteChannel channel;
    private final WarcCompression compression;
    private final ByteBuffer buffer = ByteBuffer.allocate(8192);
    ByteBuffer recordSeperator = ByteBuffer.allocate(2);
    
    private AtomicLong bytesWritten = new AtomicLong(0);

    public WarcWriter(WritableByteChannel channel, WarcCompression compression) {
        if (compression == WarcCompression.GZIP) {
            throw new UnsupportedOperationException("Writing of GZIP WARC files is not currently supported");
        }
        this.channel = channel;
        this.compression = compression;
        
        recordSeperator.put(new byte[] { '\n','\n' });
    }

    public synchronized void write(WarcRecord record) throws IOException {
        // TODO: buffer headers, compression
        bytesWritten.addAndGet(channel.write(ByteBuffer.wrap(record.serializeHeader())));
        MessageBody body = record.body();
        while (body.read(buffer) >= 0) {
            buffer.flip();
            bytesWritten.addAndGet(channel.write(buffer));
            buffer.compact();
        }
        recordSeperator.rewind();
        bytesWritten.addAndGet(channel.write(recordSeperator));
                
    }
    
    public long getCurrentWritePosition() {
        return bytesWritten.get();
    }
}
