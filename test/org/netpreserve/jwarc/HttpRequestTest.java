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

    @Test
    public void chunkedEncodingRequestShouldDecodeBody() throws IOException {
        String header = "POST /submit HTTP/1.1\r\n" +
                "Host: example.org\r\n" +
                "Transfer-Encoding: chunked\r\n\r\n";
        String chunkedBody = "4\r\nWiki\r\n" +
                "5\r\npedia\r\n" +
                "0\r\n\r\n";
        String message = header + chunkedBody;

        HttpRequest request = HttpRequest.parse(Channels.newChannel(new ByteArrayInputStream(message.getBytes(US_ASCII))));

        assertEquals("POST", request.method());
        assertEquals("/submit", request.target());
        assertEquals(Optional.of("chunked"), request.headers().first("Transfer-Encoding"));
        assertEquals("Wikipedia", new String(IOUtils.readNBytes(request.body().stream(), 32), US_ASCII));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidVersionShouldThrow() {
        new HttpRequest.Builder("GET", "/").version(MessageVersion.WARC_1_0);
    }

    @Test
    public void invalidContentLengthHeader() throws IOException {
        String header = "POST / HTTP/1.1\r\n" +
                "Connection: close\r\n" +
                "Host:   example.org\n" +
                "Content-Length: 6 dinosaurs\r\n\r\n";
        String message = header + "[body]";
        HttpRequest request = HttpRequest.parse(LengthedBody.create(message.getBytes(US_ASCII)));
        assertEquals("POST", request.method());
        assertEquals("[body]", new String(IOUtils.readNBytes(request.body().stream(), 10)));
    }
}