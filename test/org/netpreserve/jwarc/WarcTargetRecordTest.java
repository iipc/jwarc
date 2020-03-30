package org.netpreserve.jwarc;

import org.junit.Test;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

public class WarcTargetRecordTest {
    @Test
    public void testTargetURIAngleBracketsQuirk() { // per warc 1.0 grammar
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("WARC-Target-URI", Collections.singletonList("<http://example.org/>"));
        WarcTargetRecord record = new WarcTargetRecord(MessageVersion.WARC_1_0, new MessageHeaders(headers), MessageBody.empty()) {
        };
        assertEquals("http://example.org/", record.target());
        assertEquals(URI.create("http://example.org/"), record.targetURI());
    }

    @Test
    public void testTargetURINormal() { // per warc 1.1 (and warc 1.0 examples)
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("WARC-Target-URI", Collections.singletonList("http://example.org/"));
        WarcTargetRecord record = new WarcTargetRecord(MessageVersion.WARC_1_0, new MessageHeaders(headers), MessageBody.empty()) {
        };
        assertEquals("http://example.org/", record.target());
        assertEquals(URI.create("http://example.org/"), record.targetURI());
    }
}