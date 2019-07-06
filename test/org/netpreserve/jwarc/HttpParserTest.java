package org.netpreserve.jwarc;

import org.junit.Test;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpParserTest {

    @Test
    public void nonAsciiCharsShouldNotError() {
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
        HttpParser httpParser = new HttpParser();
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(UTF_8));
        httpParser.parse(buffer);
        assertFalse(httpParser.isError());
        assertTrue(httpParser.isFinished());
    }
}