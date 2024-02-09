/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018-2022 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Ignore;
import org.junit.Test;
import org.netpreserve.jwarc.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.junit.Assert.*;

public class WarcReaderTest {
    @Test
    public void emptyFileShouldReturnNoRecords() throws IOException {
        WarcReader reader = new WarcReader(Channels.newChannel(new ByteArrayInputStream(new byte[0])));
        assertFalse(reader.iterator().hasNext());
        assertFalse(reader.next().isPresent());
        assertEquals(0, reader.records().count());
        reader.close();
    }

    /**
     * Ensure incomplete gzipped WARC file/record throws an exception. Set a
     * (generous) time limit to fail on hang-ups.
     */
    @Test(timeout = 60000)
    public void incompleteGzippedWarcRecordShouldCauseException() throws IOException, URISyntaxException {
        String warcFileName = "cc.warc.gz"; // test a single-record WARC file
        byte[] gzipped = readWarcFile(warcFileName);

        // verify that unclipped WARC file is properly processed
        WarcReader reader1 = new WarcReader(Channels
                .newChannel(new ByteArrayInputStream(gzipped)));
        Optional<WarcRecord> record = reader1.next();
        assertTrue(record.isPresent());
        record.get().body().consume();
        assertFalse("Only one record in WARC file", reader1.next().isPresent());
        reader1.close();

        // clip WARC file byte by byte to check for boundary conditions
        for (int i = 1; i < gzipped.length; i++) {
            try (WarcReader reader2 = new WarcReader(Channels
                    .newChannel(new ByteArrayInputStream(Arrays.copyOfRange(gzipped, 0, (gzipped.length - i)))))) {

                record = reader2.next();

                // read entire record to force an IOException on clipped input
                record.get().body().consume();

                // progress to next record (not existing) which may also trigger the IOException
                assertFalse("Only one record in WARC file", reader2.next().isPresent());

                fail("Expected IOException on incomplete gzip (clipped by " + i + " bytes)");
            } catch (IOException e) {
                // ok: a clipped gzipped WARC file is expected to trigger an IOException
            }
        }
    }

    private byte[] readWarcFile(String warcFileName) throws IOException, URISyntaxException {
        URL warcFile = getClass().getClassLoader().getResource("org/netpreserve/jwarc/" + warcFileName);
        assertNotNull("WARC file " + warcFileName + " not found", warcFile);
        return Files.readAllBytes(Paths.get(warcFile.toURI()));
    }

    private void parseGzippedWithBuffer(ByteBuffer buffer) throws IOException, URISyntaxException {
        String warcFileName = "cc.warc.gz"; // test a single-record WARC file
        byte[] gzipped = readWarcFile(warcFileName);

        WarcReader reader = new WarcReader(Channels
                .newChannel(new ByteArrayInputStream(gzipped)), buffer);
        Optional<WarcRecord> record = reader.next();
        reader.close();
        assertTrue(record.isPresent());
        assertTrue(record.get() instanceof WarcResponse);
    }

    @Test(expected = IllegalArgumentException.class)
    public void externalBufferNoArray() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192).asReadOnlyBuffer();
        buffer.flip();
        parseGzippedWithBuffer(buffer);
    }

    @Ignore("User must ensure buffer is in read state")
    @Test
    public void externalBufferNoReadState() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        // not calling buffer.flip()
        parseGzippedWithBuffer(buffer);
    }

    @Test
    public void externalBufferByteOrderLE() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.flip();
        parseGzippedWithBuffer(buffer);
    }

    @Test
    public void externalBufferByteOrderBE() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.flip();
        parseGzippedWithBuffer(buffer);
    }

    @Test
    public void externalBufferPrepopulated() throws IOException, URISyntaxException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        String warcFileName = "cc.warc.gz"; // test a single-record WARC file
        byte[] gzipped = readWarcFile(warcFileName);

        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(gzipped));

        // read an arbitrary number of bytes into the buffer
        int bytesToBuffer = (int) (Math.random() * gzipped.length);
        buffer.limit(bytesToBuffer);
        channel.read(buffer);
        buffer.flip();
        String failureMessage = "Failed testing pre-populated buffer of " + bytesToBuffer + " bytes length";
        System.out.println(failureMessage);

        try {
            WarcReader reader = new WarcReader(channel, buffer);
            Optional<WarcRecord> record = reader.next();
            reader.close();
            assertTrue(failureMessage, record.isPresent());
            assertTrue(record.get() instanceof WarcResponse);
        } catch (Throwable e) {
            fail(failureMessage);
        }
    }

    @Test
    public void testReadingFileChannel() throws IOException, URISyntaxException {
        byte[] data = readWarcFile("cc.warc.gz");
        Path temp = Files.createTempFile("jwarc", ".tmp");
        try {
            Files.write(temp, data);
            try (GZIPOutputStream stream = new GZIPOutputStream(Files.newOutputStream(temp, APPEND))) {
                stream.write(WarcResponseTest.warc.getBytes(UTF_8));
            }
            try (WarcReader reader = new WarcReader(temp)) {
                {
                    WarcResponse response = (WarcResponse) reader.next().get();
                    byte[] buf = new byte[8192];
                    InputStream stream = response.http().body().stream();
                    int total = 0;
                    for (int n = stream.read(buf); n >= 0; n = stream.read(buf)) {
                        total += n;
                    }
                    assertEquals(20289, total);
                }

                assertEquals("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0",
                        reader.next().get().id().toString());
                assertEquals(data.length, reader.position());
                assertFalse(reader.next().isPresent());

                reader.position(0L);
                assertEquals("urn:uuid:ececb7b0-ed4d-4d27-9ae8-2676df51cf7d",
                        reader.next().get().id().toString());
                assertEquals("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0",
                        reader.next().get().id().toString());
                assertFalse(reader.next().isPresent());

                reader.position(data.length);
                assertEquals("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0",
                        reader.next().get().id().toString());
                reader.position(data.length);
                assertEquals("urn:uuid:92283950-ef2f-4d72-b224-f54c6ec90bb0",
                        reader.next().get().id().toString());
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    public void testCalculateBlockDigest() throws IOException {
        String testWarc = "WARC/1.0\r\n" +
                "WARC-Type: warcinfo\r\n" +
                "WARC-Date: 2006-09-19T17:20:14Z\r\n" +
                "WARC-Record-ID: <urn:uuid:d7ae5c10-e6b3-4d27-967d-34780c58ba39>\r\n" +
                "WARC-Filename: hello.warc\r\n" +
                "WARC-Block-Digest: sha1:PQRC7MUSPWBIV4RPLEQTJ2ETESAGG7AN\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 8\r\n" +
                "\r\n" +
                "12345678" +
                "\r\n\r\n";
        WarcReader reader = new WarcReader(new ByteArrayInputStream(testWarc.getBytes(UTF_8)));
        reader.calculateBlockDigest();
        WarcRecord record = reader.next().orElseThrow(AssertionError::new);
        assertEquals("sha1:PQRC7MUSPWBIV4RPLEQTJ2ETESAGG7AN", record.calculatedBlockDigest()
                .orElseThrow(AssertionError::new).toString());
    }

    @Test
    public void testReadingBackTwice() throws IOException {
        Path temp = Files.createTempFile("test", ".warc");
        try {
            Files.write(temp, ("WARC/1.0\r\n" +
                               "WARC-Type: revisit\r\n" +
                               "WARC-Target-URI: https://example.com/\r\n" +
                               "WARC-Date: 2018-01-01T09:11:04Z\r\n" +
                               "WARC-Record-ID: <urn:uuid:31cb2de4-9962-11ee-b9d1-0242ac120002>\r\n" +
                               "Content-Length: 42\r\n" +
                               "Content-Type: application/http;msgtype=response\r\n" +
                               "\r\n" +
                               "HTTP/1.1 200 OK\r\n" +
                               "Content-Length: 12\r\n" +
                               "\r\n").getBytes(UTF_8));

            try (WarcReader reader = new WarcReader(temp)) {
                for (WarcRecord record : reader) {
                    WarcRevisit revisit = (WarcRevisit) record;
                    try (MessageBody body = revisit.http().body()) {
                        byte[] data = IOUtils.readNBytes(body.stream(), 1024);
                        assertEquals(0, data.length);
                    }
                }
            }

            try (WarcReader reader = new WarcReader(temp)) {
                for (WarcRecord record : reader) {
                    WarcRevisit revisit = (WarcRevisit) record;
                    try (MessageBody body = revisit.http().body()) {
                        byte[] data = IOUtils.readNBytes(body.stream(), 1024);
                        assertEquals(0, data.length);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    public void testWarcioArcVersionBlockLengthVariant() throws Exception {
        String testArc = "filedesc://example.arc 0.0.0.0 20240101000000 text/plain 75\n" +
                         "1 0 Warcio testcase\n" +
                         "URL IP-address Archive-date Content-type Archive-length\n" +
                         "\n" +
                         "http://example.com/1 1.2.3.4 20240101000001 text/html 0\n" +
                         "http://example.com/2 4.3.2.1 20240101000002 text/html 1\nx\n\n\n" +
                         "http://example.com/3 4.3.2.1 20240101000002 text/html 0\n";
        WarcReader reader = new WarcReader(new ByteArrayInputStream(testArc.getBytes(UTF_8)));
        WarcRecord record = reader.next().orElseThrow(AssertionError::new);
        assertEquals(Optional.of("example.arc"), ((Warcinfo)record).filename());
        record = reader.next().orElseThrow(AssertionError::new);
        assertEquals("http://example.com/1", ((WarcResponse)record).target());
        record = reader.next().orElseThrow(AssertionError::new);
        assertEquals("http://example.com/2", ((WarcResponse)record).target());
        record = reader.next().orElseThrow(AssertionError::new);
        assertEquals("http://example.com/3", ((WarcResponse)record).target());
        assertFalse(reader.next().isPresent());
    }

    @Test
    public void testArcFileFormatReferenceExample() throws Exception {
        // missing linefeeds were added before "Last-modified:" and "<HTML>" to make the HTTP response valid
        String testArc = "filedesc://IA-001102.arc 0 19960923142103 text/plain 76\n" +
                         "1 0 Alexa Internet\n" +
                         "URL IP-address Archive-date Content-type Archive-length\n" +
                         "\n" +
                         "http://www.dryswamp.edu:80/index.html 127.10.100.2 19961104142103 text/html 202\n" +
                         "HTTP/1.0 200 Document follows\n" +
                         "Date: Mon, 04 Nov 1996 14:21:06 GMT\n" +
                         "Server: NCSA/1.4.1\n" +
                         "Content-type: text/html\n" +
                         "Last-modified: Sat,10 Aug 1996 22:33:11 GMT\n" +
                         "Content-length: 30\n\n" +
                         "<HTML>\n" +
                         "Hello World!!!\n" +
                         "</HTML>\n" +
                         "\n" +
                         "filedesc://IA-001102.arc 0.0.0.0 19960923142103 text/plain 200 - - 0 IA-001102.arc 122\n" +
                         "2 0 Alexa Internet\n" +
                         "URL IP-address Archive-date Content-type Result-code Checksum\n" +
                         "Location Offset Filename Archive-length\n" +
                         "\n" +
                         "http://www.dryswamp.edu:80/index.html 127.10.100.2 19961104142103 text/html 200 fac069150613fe55599cc7fa88aa089d - 209 IA-001102.arc 202\n" +
                         "HTTP/1.0 200 Document follows\n" +
                         "Date: Mon, 04 Nov 1996 14:21:06 GMT\n" +
                         "Server: NCSA/1.4.1\n" +
                         "Content-type: text/html\n" +
                         "Last-modified: Sat,10 Aug 1996 22:33:11 GMT\n" +
                         "Content-length: 30\n\n" +
                         "<HTML>\n" +
                         "Hello World!!!\n" +
                         "</HTML>\n";
        WarcReader reader = new WarcReader(new ByteArrayInputStream(testArc.getBytes(UTF_8)));
        {
            Warcinfo record = (Warcinfo) reader.next().orElseThrow(AssertionError::new);
            assertEquals(Optional.of("IA-001102.arc"), record.filename());
            assertEquals(Instant.parse("1996-09-23T14:21:03Z"), record.date());
        }
        {
            WarcResponse record = (WarcResponse) reader.next().orElseThrow(AssertionError::new);
            assertEquals("http://www.dryswamp.edu:80/index.html", record.target());
            HttpResponse http = record.http();
            assertEquals(200, http.status());
            assertEquals(Optional.of("Sat,10 Aug 1996 22:33:11 GMT"), http.headers().sole("Last-modified"));
            MessageBody payloadBody = record.payload().get().body();
            String payloadString = new String(IOUtils.readNBytes(payloadBody.stream(), (int)payloadBody.size()), UTF_8);
            assertEquals("<HTML>\nHello World!!!\n</HTML>", payloadString);
        }
        {
            Warcinfo record = (Warcinfo) reader.next().orElseThrow(AssertionError::new);
            assertEquals(Optional.of("IA-001102.arc"), record.filename());
        }
        {
            WarcResponse record = (WarcResponse) reader.next().orElseThrow(AssertionError::new);
            assertEquals("http://www.dryswamp.edu:80/index.html", record.target());
        }
        assertFalse(reader.next().isPresent());
    }
}
