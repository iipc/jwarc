package jwarc;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WarcParserTest {

    @Test
    public void test() {
        String data = "WARC/100.1\r\n" +
                "WARC-Type: response\r\n" +
                "WARC-Target-URI: http://www.archive.org/images/logoc.jpg\r\n" +
                "WARC-Warcinfo-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
                "WARC-Date: 2006-09-19T17:20:24Z\r\n" +
                "WARC-Block-Digest: sha1:UZY6ND6CCHXETFVJD2MSS7ZENMWF7KQ2\r\n" +
                "WARC-Payload-Digest: sha1:CCHXETFVJD2MUZY6ND6SS7ZENMWF7KQ2\r\n" +
                "WARC-IP-Address: 207.241.233.58\r\n" +
                "WARC-Record-ID: <urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0>\r\n" +
                "Content-Type: application/http;msgtype=response\r\n" +
                "Folding-Test: hello\r\n" +
                "\t there my\r\n" +
                " world\r\n" +
                "WARC-Identified-Payload-Type: image/jpeg\r\n" +
                "Content-Length: 17\r\n" +
                "\r\n" +
                "this is the block\r\n";
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        WarcParser parser = new WarcParser();

        parser.feed(buf);

        assertTrue(parser.isFinished());
        assertFalse(parser.isError());

        assertEquals(data.indexOf("this is the block"), buf.position());

        assertEquals(100, parser.versionMajor);
        assertEquals(1, parser.versionMinor);

        assertEquals(12, parser.fields.size());
        assertEquals("hello\nthere my\nworld", parser.fields.get("Folding-Test"));
    }
}
