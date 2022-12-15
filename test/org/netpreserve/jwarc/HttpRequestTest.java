package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

public class HttpRequestTest {
    @Test
    public void serializeHeaderShouldPreserveExactly() throws IOException {
        String header = "POST / HTTP/1.1\r\n" +
                "Connection: close\r\n" +
                "Host:   example.org\n" +
                "Content-Length: 6\r\n\r\n";
        String message = header + "[body]";
        HttpRequest request = HttpRequest.parse(Channels.newChannel(new ByteArrayInputStream(message.getBytes(US_ASCII))));
        assertEquals("POST", request.method());
        assertEquals("/", request.target());
        assertEquals(Optional.of("example.org"), request.headers().first("Host"));
        assertEquals(header, new String(request.serializeHeader(), US_ASCII));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidVersionShouldThrow() {
        new HttpRequest.Builder("GET", "/").version(MessageVersion.WARC_1_0);
    }
}