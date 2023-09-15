package org.netpreserve.jwarc;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.*;

public class WarcParserTest {
    @Test
    public void testParsingArcWithBogusMime() {
        WarcParser parser = parse("http://example.com/ 1.2.3.4 20110104111607 @[=*ï¿½Content-Type] 494\n");
        assertEquals(Optional.of("494"), parser.headers().sole("Content-Length"));
        parser = parse("http://example.com/ 1.2.3.4 20110104111607 charset=foo 494\n");
        assertEquals(Optional.of("494"), parser.headers().sole("Content-Length"));
        parser = parse("http://example.com/ 1.2.3.4 20110104111607 image(jpeg) 494\n");
        assertEquals(Optional.of("494"), parser.headers().sole("Content-Length"));
        parser = parse("http://example.com/ 1.2.3.4 20110104111607 ERROR: 494\n");
        assertEquals(Optional.of("494"), parser.headers().sole("Content-Length"));
    }

    @Test
    public void testParsingArcWithCorruptDates() {
        WarcParser parser = parse("http://example.com/ 1.2.3.4 200012120739 text/html 42\n");
        assertEquals(Optional.of("2000-12-12T07:39:00Z"), parser.headers().first("WARC-Date"));
        parser = parse("http://example.com/ 1.2.3.4 2000121207394211 text/html 1942\n");
        assertEquals(Optional.of("2000-12-12T07:39:42Z"), parser.headers().first("WARC-Date"));
        parser = parse("http://example.com/ 1.2.3.4 99999999999999 text/html 1942\n");
        assertEquals(Optional.empty(), parser.headers().first("WARC-Date"));
    }

    private static WarcParser parse(String input) {
        WarcParser parser = new WarcParser();
        parser.parse(ByteBuffer.wrap(input.getBytes(StandardCharsets.ISO_8859_1)));
        assertFalse(parser.isError());
        assertTrue(parser.isFinished());
        return parser;
    }
}