package org.netpreserve.jwarc.cdx;

import org.junit.Test;
import org.netpreserve.jwarc.HttpResponse;
import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.WarcResponse;
import org.netpreserve.jwarc.WarcRevisit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.Assert.*;

public class CdxFormatTest {
    @Test
    public void test() throws IOException {
        Path path=Paths.get("/home/jwarc/example.warc.gz");
        
        HttpResponse httpResponse = new HttpResponse.Builder(404, "Not Found")
                .body(MediaType.HTML, new byte[0])
                .build();
        WarcResponse response = new WarcResponse.Builder("http://example.org/")
                .date(Instant.parse("2022-03-02T21:44:34Z"))
                .payloadDigest("sha1", "AQLNJ7DOPHK477BWWC726H7Y5XBPBNF7")
                .body(httpResponse)
                .build();
        assertEquals("org,example)/ 20220302214434 http://example.org/ text/html 404 AQLNJ7DOPHK477BWWC726H7Y5XBPBNF7 - - 456 123 example.warc.gz",
                CdxFormat.CDX11.format(response, path, 123, 456));
    }

    @Test
    public void testDigestUnchanged() throws Exception {
        Path path=Paths.get("/home/jwarc/example.warc.gz");
        
        CdxFormat cdxFormat = new CdxFormat.Builder()
                .digestUnchanged() // We want the digest as is.
                .build();
        String payloadDigest="sha256:b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3";
                
        HttpResponse httpResponse = new HttpResponse.Builder(404, "Not Found")
                .body(MediaType.HTML, new byte[0])
                .build();
        WarcResponse response = new WarcResponse.Builder("http://example.org/")
                .date(Instant.parse("2022-03-02T21:44:34Z"))                                                               
                .body(httpResponse)
                .addHeader("WARC-Payload-Digest", payloadDigest) 
                .build();
        assertEquals("org,example)/ 20220302214434 http://example.org/ text/html 404 "+payloadDigest+" - - 456 123 example.warc.gz",                      
                cdxFormat.format(response, path , 123, 456));
    }

    
    @Test
    public void testRevisit() throws Exception {
        Path path=Paths.get("/home/jwarc/example.warc.gz");
        
        CdxFormat cdxFormat = new CdxFormat.Builder()
                .digestUnchanged() // We want the digest as is.
                .revisistsIncluded() 
                .build();
        String payloadDigest="sha256:b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3";
                
        HttpResponse httpResponse = new HttpResponse.Builder(404, "Not Found")
                .body(MediaType.HTML, new byte[0])
                .build();
        WarcRevisit revisit = new WarcRevisit.Builder("http://example.org/")
                .date(Instant.parse("2022-03-02T21:44:34Z"))                
                .body(httpResponse)
                .addHeader("WARC-Payload-Digest", payloadDigest)
                .addHeader("WARC-Type", "revisit")
                .build();
        assertEquals("org,example)/ 20220302214434 http://example.org/ warc/revisit 404 "+payloadDigest+" - - 456 123 example.warc.gz",                      
                cdxFormat.format(revisit, path, 123, 456));
    }
   
    @Test
    public void testFullFilePath() throws Exception {
        Path path=Paths.get("/home/jwarc/example.warc.gz");
        
        CdxFormat cdxFormat = new CdxFormat.Builder()
                .digestUnchanged() // We want the digest as is.
                .fullFilePath() // Full filepath, not just filename
                .build();
        String payloadDigest="sha256:b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3";
                
        HttpResponse httpResponse = new HttpResponse.Builder(404, "Not Found")
                .body(MediaType.HTML, new byte[0])
                .build();
        WarcResponse response = new WarcResponse.Builder("http://example.org/")
                .date(Instant.parse("2022-03-02T21:44:34Z"))                                                               
                .body(httpResponse)                
                .addHeader("WARC-Payload-Digest", payloadDigest) 
                .build();
        assertEquals("org,example)/ 20220302214434 http://example.org/ text/html 404 "+payloadDigest+" - - 456 123 /home/jwarc/example.warc.gz",                      
                cdxFormat.format(response, path, 123, 456));
    }

    
    
}