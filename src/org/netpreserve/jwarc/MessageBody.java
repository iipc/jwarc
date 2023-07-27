package org.netpreserve.jwarc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public abstract class MessageBody extends MessageParser implements ReadableByteChannel {

    MessageBody() {
    }

    public static MessageBody empty() {
        return LengthedBody.EMPTY;
    }

    /**
     * Returns the length of the body. This may be less than the Content-Length header if the record was truncated.
     * Returns -1 if the length cannot be determined (such as when chunked encoding is used).
     */
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
