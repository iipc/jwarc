package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.*;
import static org.junit.Assert.assertEquals;

public class LengthedBodyTest {

    @Test
    public void test() throws IOException {
        Path temp = Files.createTempFile("jwarc-test", ".tmp");
        try (FileChannel channel = FileChannel.open(temp, DELETE_ON_CLOSE, WRITE, READ)) {
            channel.write(ByteBuffer.wrap("xx0123456789yy".getBytes(US_ASCII)));
            channel.position(2);
            ByteBuffer buf = ByteBuffer.allocate(2);
            buf.flip();
            SeekableByteChannel body = (SeekableByteChannel) LengthedBody.create(channel, buf, channel.size() - 4);
            {
                ByteBuffer b = ByteBuffer.allocate(32);
                while (true) {
                    if (body.read(b) < 0) break;
                }
                b.flip();
                assertEquals("0123456789", US_ASCII.decode(b).toString());
            }

            {
                body.position(3);
                ByteBuffer b = ByteBuffer.allocate(4);
                body.read(b);
                b.flip();
                assertEquals("3456", US_ASCII.decode(b).toString());
            }

        }
    }

}