package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.*;

public class HttpParserTest {
    private final HttpParser httpParser = new HttpParser();

    @Test
    public void nonAsciiCharsShouldNotError() throws IOException {
        String message = "HTTP/1.1 200 OK\r\n" +
                "Date: Fri, 30 Mar 2018 14:24:06 GMT\r\n" +
                "Server: Apache/2.4.16 (Unix) OpenSSL/1.0.1e-fips mod_bwlimited/1.4\r\n" +
                "Last-Modified: Fri, 30 Mar 2018 14:23:15 GMT\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Content-Length: 1045\r\n" +
                "Cache-Control: max-age=2592000, public, public\r\n" +
                "Expires: Sun, 29 Apr 2018 14:24:06 GMT\r\n" +
                "Strict-Transport-Security: “max-age=31536000″\r\n" +
                "Vary: User-Agent\r\n" +
                "Connection: close \r\n" +
                "Content-Type: image/png\r\n" +
                "\r\n";
        httpParser.strictResponse();
        parse(message);
    }

    @Test
    public void strictRequestModeShouldBeStrict() throws IOException {
        httpParser.strictRequest();
        parseShouldFail("GET    /     HTTP/1.1  \r\nContent-Length: 0\r\n\r\n");
        parseShouldFail("GET / HTTP/1.1\nContent-Length: 0\n\n");
        parseShouldFail("GET / HTTP/1.1\r\nCookie: abc\1def\r\n\r\n");
        parseShouldFail("GET /# HTTP/1.1\r\nContent-Length: 0\r\n\r\n");
        parseShouldFail("GET /\1 HTTP/1.1\r\nContent-Length: 0\r\n\r\n");
        parseShouldFail("GET /\r\nContent-Length: 0\r\n\r\n");
    }

    @Test
    public void lenientRequestModeShouldBeLenient() throws IOException {
        httpParser.lenientRequest();
        parse("GET    /     HTTP/1.1  \r\nContent-Length: 0\r\n\r\n");
        assertEquals("/", httpParser.target());
        parse("GET / HTTP/1.1\nContent-Length: 0\n\n");
        assertEquals("/", httpParser.target());
        assertEquals(Optional.of("0"), httpParser.headers().sole("Content-Length"));
        parse("GET / HTTP/1.1\r\nCookie: abc\1def\r\n\r\n");
        assertEquals(Optional.of("abc\1def"), httpParser.headers().sole("Cookie"));
        parse("GET /#\1 HTTP/1.1\r\nContent-Length: 0\r\n\r\n");
        assertEquals("/#\1", httpParser.target());
        parse("GET /foo\r\nContent-Length: 0\r\n\r\n");
        assertEquals("/foo", httpParser.target());
        parse("GET   /foo  \r\nContent-Length: 0\r\n\r\n");
        assertEquals("/foo", httpParser.target());
    }

    @Test
    public void strictResponseModeShouldBeStrict() throws IOException {
        httpParser.strictResponse();
        parseShouldFail("HTTP/1.1 200\r\nContent-Length: 0\r\n\r\n");
        parseShouldFail("HTTP/1.1    200 OK\r\nContent-Length: 0\r\n\r\n");
        parseShouldFail("HTTP/1.1 200 OK\nContent-Length: 0\n\n");
        parseShouldFail("HTTP/1.1 200 OK\r\nSet-Cookie: abc\1def\r\n\r\n");
        parseShouldFail("HTTP/1.1 200 OK\nContent-Length  \t: 0  \n\n");
    }

    @Test
    public void lenientResponseModeShouldBeLenient() throws IOException {
        httpParser.lenientResponse();
        parse("HTTP/1.1 200\r\nContent-Length: 0\r\n\r\n");
        assertEquals(null, httpParser.reason());
        parse("HTTP/1.1    200 OK\r\nContent-Length: 0\r\n\r\n");
        assertEquals("OK", httpParser.reason());
        parse("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nFolded: 1 \r\n 2  \r\n\r\n");
        assertEquals(Optional.of("1 2"), httpParser.headers().sole("Folded"));
        parse("HTTP/1.1 200 OK\nContent-Length: 0\n\n");
        parse("HTTP/1.1 200 OK\r\nSet-Cookie: abc\1def\r\n\r\n");
        assertEquals(Optional.of("abc\1def"), httpParser.headers().sole("Set-Cookie"));
        parse("HTTP/1.1 200 OK\nContent-Length  \t: 0  \n\n");
        assertEquals(Optional.of("0"), httpParser.headers().sole("Content-Length"));
        parse("HTTP/1.1 200 OK\nbroken \tfield  \t: yes  \n\n");
        assertEquals(Optional.of("yes"), httpParser.headers().sole("broken \tfield"));
        parse("HTTP/1.1 200 OK\n<span class=\"nonsense\">nonsense</span>: yes  \n\n");
        assertEquals(Optional.of("yes"), httpParser.headers().sole("<span class=\"nonsense\">nonsense</span>"));
        parse("HTTP/1.1 200 OK\n: yes  \n\n");
        assertEquals(Optional.of("yes"), httpParser.headers().sole(""));
        parse("HTTP/1.1 200\r\nKey: value");
        assertEquals(Optional.of("value"), httpParser.headers().sole("Key"));
        parse("HTTP/1.1 200\r\nKey: value\r\nkey2: value2\r\n");
        assertEquals(Optional.of("value2"), httpParser.headers().sole("key2"));
    }

    @Test
    public void shouldStopBeforeBody() throws IOException {
        httpParser.lenientResponse();
        assertEquals(6, parse("HTTP/1.1 200\r\nContent-Length: 0\r\n\r\n[body]"));
        assertEquals(6, parse("HTTP/1.1 200\r\nContent-Length: 0\n\n[body]"));
        httpParser.strictResponse();
        assertEquals(6, parse("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n[body]"));
        httpParser.strictRequest();
        assertEquals(6, parse("GET / HTTP/1.1\r\n\r\n[body]"));
        httpParser.lenientRequest();
        assertEquals(6, parse("GET / HTTP/1.1\r\n\r\n[body]"));
        assertEquals(6, parse("GET /\n\n[body]"));
    }

    private int parse(String message) throws IOException {
        httpParser.reset();
        ByteBuffer buffer = wrap(message);
        httpParser.parse(Channels.newChannel(new ByteArrayInputStream(new byte[0])), buffer);
        assertFalse(httpParser.isError());
        assertTrue(httpParser.isFinished());
        return buffer.remaining();
    }

    private void parseShouldFail(String message) throws IOException {
        httpParser.reset();
        httpParser.parse(wrap(message));
        assertTrue(httpParser.isError());
    }

    private ByteBuffer wrap(String message) {
        return ByteBuffer.wrap(message.getBytes(ISO_8859_1));
    }
}