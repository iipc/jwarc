package org.netpreserve.jwarc.cdx;

import org.junit.Test;
import org.netpreserve.jwarc.MediaType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;

import static org.junit.Assert.*;

public class CdxReaderTest {
    @Test
    public void test() throws IOException {
        String data = "- 20220302214434 http://example.org/ text/html 200 AQLNJ7DOPHK477BWWC726H7Y5XBPBNF7 - - 1062 760582405 example.warc.gz\n" +
                "- 20220302214433 https://example.org/page/ application/rss+xml 200 AQO24VNPMHIM6GUNVSCP7IUUETZ4U52J - - 971 760584354 example.warc.gz\n" +
                "- 20220302214434 https://example.org/style.css text/css 200 AG2PTU7G6DMXCBP6IBSR5VG5RUMYOHHN - - 749 760586303 example.warc.gz\n";

        try (CdxReader reader = new CdxReader(new BufferedReader(new StringReader(data)))) {
            CdxRecord record = reader.next().get();
            assertEquals(200, (int) record.status());
            assertEquals("http://example.org/", record.target());
            assertEquals("AQLNJ7DOPHK477BWWC726H7Y5XBPBNF7", record.digest());
            assertEquals(760582405, (long) record.position());
            assertEquals(1062, (long) record.size());
            assertEquals("example.warc.gz", record.filename());
            assertEquals(Instant.parse("2022-03-02T21:44:34Z"), record.date());
            assertEquals(MediaType.HTML, record.contentType());

            assertTrue(reader.next().isPresent());
            assertTrue(reader.next().isPresent());
            assertFalse(reader.next().isPresent());
        }
    }
}