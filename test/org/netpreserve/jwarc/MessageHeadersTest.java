package org.netpreserve.jwarc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class MessageHeadersTest {
    @Test
    public void testContains() {
        assertFalse(headers("Z", "1")
                .contains("Transfer-Encoding", "chunked"));
        assertTrue(headers("A", "0", "Transfer-Encoding", "chunked", "Z", "1")
                .contains("Transfer-Encoding", "chunked"));
        assertFalse(headers("Transfer-Encoding", "xchunkedx")
                .contains("Transfer-Encoding", "chunked"));
        assertFalse(headers("Transfer-Encoding", "gzip chunked")
                .contains("Transfer-Encoding", "chunked"));
        assertTrue(headers("Transfer-Encoding", "gzip, chunked, chunked, gzip")
                .contains("Transfer-Encoding", "chunked"));
        assertTrue(headers("Transfer-Encoding", "gzip,     \tCHUNKED,,, GZIP")
                .contains("Transfer-Encoding", "Chunked"));
    }

    private static MessageHeaders headers(String... headers) {
        Map<String,List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < headers.length; i += 2) {
            map.computeIfAbsent(headers[i], (k) -> new ArrayList<>()).add(headers[i + 1]);
        }
        return new MessageHeaders(map);
    }
}