package org.netpreserve.jwarc;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.*;

public class WarcParserTest {
    @Test
    public void testParsingArcWithBogusMime() {
        String input = "http://example.com/ 1.2.3.4 20110104111607 @[=*ï¿½Content-Type] 494\n";
        WarcParser parser = new WarcParser();
        parser.parse(ByteBuffer.wrap(input.getBytes(StandardCharsets.ISO_8859_1)));
        assertFalse(parser.isError());
        assertTrue(parser.isFinished());
        assertEquals(Optional.of("494"), parser.headers().sole("Content-Length"));
    }
}