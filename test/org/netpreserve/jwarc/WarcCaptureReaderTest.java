/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class WarcCaptureReaderTest {

    @Test
    public void test() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WarcWriter writer = new WarcWriter(Channels.newChannel(baos));

        writer.write(new Warcinfo.Builder().filename("test.warc").build());

        URI target = URI.create("http://example.org/");

        WarcRequest request = new WarcRequest.Builder(target)
                .build();
        writer.write(request);

        WarcMetadata metadata = new WarcMetadata.Builder()
                .targetURI(target)
                .concurrentTo(request.id())
                .build();
        writer.write(metadata);

        WarcResponse response = new WarcResponse.Builder(target)
                .concurrentTo(request.id())
                .build();
        writer.write(response);

        // A second capture
        URI target2 = URI.create("http://example.com/");
        WarcRequest request2 = new WarcRequest.Builder(target2)
                .build();
        writer.write(request2);

        WarcResponse response2 = new WarcResponse.Builder(target2)
                .concurrentTo(request2.id())
                .build();
        writer.write(response2);

        WarcCaptureReader reader = new WarcCaptureReader(new ByteArrayInputStream(baos.toByteArray()));
        WarcCapture capture = reader.next().get();
        assertEquals(Optional.of("test.warc"), capture.warcinfo().get().filename());
        assertEquals(target.toString(), capture.target());
        List<WarcCaptureRecord> records = capture.records();
        assertEquals(3, records.size());
        assertTrue(records.get(0) instanceof WarcRequest);
        assertTrue(records.get(1) instanceof WarcMetadata);
        assertTrue(records.get(2) instanceof WarcResponse);

        capture = reader.next().get();
        assertEquals(target2.toString(), capture.target());
        records = capture.records();
        assertEquals(2, records.size());
        assertTrue(records.get(0) instanceof WarcRequest);
        assertTrue(records.get(1) instanceof WarcResponse);

        assertFalse(reader.next().isPresent());
    }

    @Test
    public void testRevisit() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WarcWriter writer = new WarcWriter(Channels.newChannel(baos));

        URI target = URI.create("http://example.org/");

        WarcRequest request = new WarcRequest.Builder(target)
                .build();
        writer.write(request);

        WarcRevisit revisit = new WarcRevisit.Builder(target, WarcRevisit.IDENTICAL_PAYLOAD_DIGEST_1_0)
                .concurrentTo(request.id())
                .build();
        writer.write(revisit);

        WarcCaptureReader reader = new WarcCaptureReader(new ByteArrayInputStream(baos.toByteArray()));
        WarcCapture capture = reader.next().get();
        assertEquals(target.toString(), capture.target());
        List<WarcCaptureRecord> records = capture.records();
        assertEquals(2, records.size());
        assertTrue(records.get(0) instanceof WarcRequest);
        assertTrue(records.get(1) instanceof WarcRevisit);
    }

    @Test
    public void testMetadataFirst() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WarcWriter writer = new WarcWriter(Channels.newChannel(baos));

        URI target = URI.create("http://example.org/");

        WarcMetadata metadata = new WarcMetadata.Builder()
                .targetURI(target)
                .build();
        writer.write(metadata);

        WarcResponse response = new WarcResponse.Builder(target)
                .concurrentTo(metadata.id())
                .build();
        writer.write(response);

        WarcCaptureReader reader = new WarcCaptureReader(new ByteArrayInputStream(baos.toByteArray()));
        WarcCapture capture = reader.next().get();
        List<WarcCaptureRecord> records = capture.records();
        assertEquals(2, records.size());
        assertTrue(records.get(0) instanceof WarcMetadata);
        assertTrue(records.get(1) instanceof WarcResponse);
    }
}
