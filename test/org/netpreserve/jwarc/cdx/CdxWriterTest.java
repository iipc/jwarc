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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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

    @Test
    public void testFiltering_nonExistingType_shouldReturnNothing() throws Exception {
        Path testWarcFile = temporaryFolder.newFile().toPath().toAbsolutePath();

        try (WarcWriter warcWriter = new WarcWriter(Files.newByteChannel(testWarcFile, CREATE, WRITE))) {
            HttpRequest httpRequest = new HttpRequest.Builder("GET", "http://example.org/")
                    .build();

            warcWriter.write(new WarcRequest.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-02T21:44:34Z"))
                    .body(httpRequest)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3faaa")
                    .build());

            HttpResponse httpResponse = new HttpResponse.Builder(200, "OK")
                    .body(MediaType.HTML, new byte[0])
                    .build();

            warcWriter.write(new WarcResponse.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-01T12:44:34Z"))
                    .body(httpResponse)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3")
                    .build());
        }

        StringWriter cdxBuffer = new StringWriter();
        try (CdxWriter cdxWriter = new CdxWriter(cdxBuffer)) {
            cdxWriter.setRecordFilter(record -> record.type().equals("metadata"));
            cdxWriter.setFormat(CdxFormat.CDXJ);
            cdxWriter.setSort(true);
            cdxWriter.process(Collections.singletonList(testWarcFile), true);
        }

        List<String> splits = cdxBuffer.toString().isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(cdxBuffer.toString().split("\n"));

        assertThat(splits, hasSize(0));
    }

    @Test
    public void testFilteringResponse_shouldWork() throws Exception {
        Path testWarcFile = temporaryFolder.newFile().toPath().toAbsolutePath();

        try (WarcWriter warcWriter = new WarcWriter(Files.newByteChannel(testWarcFile, CREATE, WRITE))) {
            HttpRequest httpRequest = new HttpRequest.Builder("GET", "http://example.org/")
                    .build();

            warcWriter.write(new WarcRequest.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-02T21:44:34Z"))
                    .body(httpRequest)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3faaa")
                    .build());

            HttpResponse httpResponse = new HttpResponse.Builder(200, "OK")
                    .body(MediaType.HTML, new byte[0])
                    .build();

            warcWriter.write(new WarcResponse.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-01T12:44:34Z"))
                    .body(httpResponse)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3")
                    .build());
        }

        StringWriter cdxBuffer = new StringWriter();
        try (CdxWriter cdxWriter = new CdxWriter(cdxBuffer)) {
            cdxWriter.setRecordFilter(record -> record.type().equals("response"));
            cdxWriter.setFormat(CdxFormat.CDXJ);
            cdxWriter.setSort(true);
            cdxWriter.process(Collections.singletonList(testWarcFile), true);
        }

        List<String> splits = cdxBuffer.toString().isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(cdxBuffer.toString().split("\n"));

        assertThat(splits, hasSize(greaterThan(0)));
        assertThat(splits.get(0), not(emptyString()));
        assertThat(splits.get(0), startsWith("org,example)/ 20220301124434"));
        assertThat(splits.get(0), containsString("http://example.org/"));
        assertThat(splits.get(0), containsString("\"status\": \"200\""));

    }

    @Test
    public void testFilteringRequest_shouldWork() throws Exception {
        Path testWarcFile = temporaryFolder.newFile().toPath().toAbsolutePath();

        try (WarcWriter warcWriter = new WarcWriter(Files.newByteChannel(testWarcFile, CREATE, WRITE))) {
            HttpRequest httpRequest = new HttpRequest.Builder("GET", "http://example.org/")
                    .build();

            warcWriter.write(new WarcRequest.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-02T21:44:34Z"))
                    .body(httpRequest)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3faaa")
                    .build());

            HttpResponse httpResponse = new HttpResponse.Builder(200, "OK")
                    .body(MediaType.HTML, new byte[0])
                    .build();

            warcWriter.write(new WarcResponse.Builder("http://example.org/")
                    .date(Instant.parse("2022-03-01T12:44:34Z"))
                    .body(httpResponse)
                    .payloadDigest("sha256", "b04af472c47a8b1b5059b3404caac0e1bfb5a3c07b329be66f65cfab5ee8d3f3")
                    .build());
        }

        StringWriter cdxBuffer = new StringWriter();
        try (CdxWriter cdxWriter = new CdxWriter(cdxBuffer)) {
            cdxWriter.setRecordFilter(record -> record.type().equals("request"));
            cdxWriter.setFormat(CdxFormat.CDXJ);
            cdxWriter.setSort(true);
            cdxWriter.process(Collections.singletonList(testWarcFile), true);
        }

        List<String> splits = cdxBuffer.toString().isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(cdxBuffer.toString().split("\n"));

        assertThat(splits, hasSize(greaterThan(0)));
        assertThat(splits.get(0), not(emptyString()));
        assertThat(splits.get(0), startsWith("org,example)/ 20220302214434"));
        assertThat(splits.get(0), containsString("http://example.org/"));
        assertThat(splits.get(0), not(containsString("\"status\": \"200\"")));

    }

}