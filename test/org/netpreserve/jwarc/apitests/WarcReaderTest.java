/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018-2022 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Ignore;
import org.junit.Test;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
}