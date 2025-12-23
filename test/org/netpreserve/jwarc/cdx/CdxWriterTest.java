package org.netpreserve.jwarc.cdx;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertEquals;

public class CdxWriterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void test() throws IOException {
        Path testWarcFile = temporaryFolder.newFile().toPath().toAbsolutePath();
        try (WarcWriter warcWriter = new WarcWriter(Files.newByteChannel(testWarcFile, CREATE, WRITE))) {
            HttpResponse httpResponse = new HttpResponse.Builder(404, "Not Found")
                    .body(MediaType.HTML, new byte[0])
                    .build();
            warcWriter.write(new WarcRevisit.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-02T21:44:34Z"))
                    .body(httpResponse)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3")
                    .build());
            warcWriter.write(new WarcResponse.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-01T12:44:34Z"))
                    .body(httpResponse)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3")
                    .build());
        }

        StringWriter cdxBuffer = new StringWriter();
        try (CdxWriter cdxWriter = new CdxWriter(cdxBuffer)) {
            cdxWriter.setFormat(new CdxFormat.Builder().digestUnchanged().build());
            cdxWriter.setSort(true);
            cdxWriter.writeHeaderLine();
            cdxWriter.process(Collections.singletonList(testWarcFile), true);
        }
        assertEquals(" CDX N b a m s k r M S V g\n" +
                "org,example)/ 20220301124434 http://example.org/ text/html 404 sha256:WBFPI4WEPKFRWUCZWNAEZKWA4G73LI6APMZJXZTPMXH2WXXI2PZQ==== - - 398 397 " + testWarcFile + "\n" +
                "org,example)/ 20220302214434 http://example.org/ warc/revisit 404 sha256:WBFPI4WEPKFRWUCZWNAEZKWA4G73LI6APMZJXZTPMXH2WXXI2PZQ==== - - 397 0 " + testWarcFile + "\n",
                cdxBuffer.toString());
    }

}