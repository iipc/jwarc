package org.netpreserve.jwarc.cdx;

import org.junit.Test;
import org.netpreserve.jwarc.HttpResponse;
import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcResponse;

import java.io.IOException;
import java.time.Instant;

import static org.junit.Assert.*;

public class CdxFormatTest {
    @Test
    public void test() throws IOException {
        HttpResponse httpResponse = new HttpResponse.Builder(404, "Not Found")
                .body(MediaType.HTML, new byte[0])
                .build();
        WarcResponse response = new WarcResponse.Builder("http://example.org/")
                .date(Instant.parse("2022-03-02T21:44:34Z"))
                .payloadDigest("sha1", "AQLNJ7DOPHK477BWWC726H7Y5XBPBNF7")
                .body(httpResponse)
                .build();
        assertEquals("- 20220302214434 http://example.org/ text/html 404 AQLNJ7DOPHK477BWWC726H7Y5XBPBNF7 - - 456 123 example.warc.gz",
                CdxFormat.CDX11.format(response, "example.warc.gz", 123, 456));
    }
}