package org.netpreserve.jwarc;

import org.junit.Test;
import org.netpreserve.jwarc.ChunkedBody;
import org.netpreserve.jwarc.ParsingException;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public class ChunkedBodyTest {
    @Test
    public void test() throws IOException {
        byte[] one = "3\r\nhel\r\n0007\r\nlo ".getBytes(US_ASCII);
        byte[] two = "worl\r\n1\r\nd\r\n00000\r\n\r\n".getBytes(US_ASCII);
        ReadableByteChannel chan = Channels.newChannel(new ByteArrayInputStream(two));
        ByteBuffer b1 = ByteBuffer.wrap(one);
        ChunkedBody decoder = new ChunkedBody(chan, b1);
        ByteBuffer buf = ByteBuffer.allocate(32);
        while (true) {
            int n = decoder.read(buf);
            assertNotEquals(0, buf);
            if (n == -1) {
                break;
            }
        }
        assertFalse(b1.hasRemaining());
        assertEquals("hello world", new String(Arrays.copyOf(buf.array(), buf.position()), US_ASCII));
    }

    @Test(expected = ParsingException.class)
    public void testErr() throws IOException {
        new ChunkedBody(Channels.newChannel(new ByteArrayInputStream(new byte[0])), ByteBuffer.allocate(16))
                .read(ByteBuffer.allocate(32));
    }

    @Test(expected = EOFException.class)
    public void testEOF() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.flip();
        new ChunkedBody(Channels.newChannel(new ByteArrayInputStream(new byte[0])), buf)
                .read(ByteBuffer.allocate(32));
    }
}