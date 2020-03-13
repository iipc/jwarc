package org.netpreserve.jwarc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public abstract class MessageBody extends MessageParser implements ReadableByteChannel {
    MessageBody() {
    }

    static MessageBody empty() {
        return LengthedBody.create(Channels.newChannel(new ByteArrayInputStream(new byte[0])),
                ByteBuffer.allocate(0), 0);
    }

    public long size() throws IOException {
        return -1;
    }

    public abstract long position() throws IOException;

    public InputStream stream() throws IOException {
        return Channels.newInputStream(this);
    }

    public void consume() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        while (read(buffer) >= 0) {
            buffer.clear();
        }
    }
}
