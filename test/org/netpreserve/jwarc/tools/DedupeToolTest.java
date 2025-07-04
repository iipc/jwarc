/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia
 */

package org.netpreserve.jwarc.tools;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;

import static java.nio.file.StandardOpenOption.*;
import static org.junit.Assert.*;

public class DedupeToolTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DedupeTool dedupeTool;
    private Path testWarcFile;
    private Path outputWarcFile;

    @Before
    public void setUp() throws IOException {
        dedupeTool = new DedupeTool();
        testWarcFile = createTestWarcFile();
        outputWarcFile = temporaryFolder.newFile("output.warc.gz").toPath();
    }

    /**
     * Creates a test WARC file with multiple records for testing deduplication.
     */
    private Path createTestWarcFile() throws IOException {
        Path file = temporaryFolder.newFile("test.warc.gz").toPath();

        try (FileChannel channel = FileChannel.open(file, CREATE, WRITE, TRUNCATE_EXISTING);
             WarcWriter writer = new WarcWriter(channel)) {

            // Create first response record
            String payload1 = "This is test content 1";
            WarcResponse response1 = createTestResponse("http://example.com/1", payload1, Instant.parse("2023-01-01T00:00:00Z"));
            writer.write(response1);

            // Create second response record with the same content (should be deduplicated)
            WarcResponse response2 = createTestResponse("http://example.com/2", payload1, Instant.parse("2023-01-02T00:00:00Z"));
            writer.write(response2);

            // Create third response record with different content (should not be deduplicated)
            String payload3 = "This is different test content";
            WarcResponse response3 = createTestResponse("http://example.com/3", payload3, Instant.parse("2023-01-03T00:00:00Z"));
            writer.write(response3);
        }

        return file;
    }

    /**
     * Helper method to create a test WarcResponse with the given URL, payload, and date.
     */
    private WarcResponse createTestResponse(String url, String payload, Instant date) throws IOException {
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + payload.length() + "\r\n" +
                "\r\n" +
                payload;

        // Calculate the payload digest
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(payload.getBytes(StandardCharsets.UTF_8));
            WarcDigest payloadDigest = new WarcDigest(md);

            return new WarcResponse.Builder(URI.create(url))
                    .date(date)
                    .body(MediaType.HTTP_RESPONSE, httpResponse.getBytes(StandardCharsets.UTF_8))
                    .payloadDigest(payloadDigest)
                    .build();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("Failed to calculate digest", e);
        }
    }

    @Test
    public void testDeduplicateWarcFile() throws Exception {
        // Set cache size to enable deduplication
        dedupeTool.setCacheSize(10);

        // Set minimum size to a small value to allow deduplication of our test payloads
        dedupeTool.setMinimumSize(10);

        // Run deduplication
        dedupeTool.deduplicateWarcFile(testWarcFile, outputWarcFile);

        // Verify the output file exists
        assertTrue("Output file should exist", Files.exists(outputWarcFile));

        // Count the records in the output file and verify deduplication
        int responseCount = 0;
        int revisitCount = 0;

        try (FileChannel channel = FileChannel.open(outputWarcFile, READ);
             WarcReader reader = new WarcReader(channel)) {

            for (WarcRecord record : reader) {
                if (record instanceof WarcResponse) {
                    responseCount++;
                } else if (record instanceof WarcRevisit) {
                    revisitCount++;
                }
            }
        }

        // We expect 2 response records (the first and third) and 1 revisit record (the second)
        assertEquals("Should have 2 response records", 2, responseCount);
        assertEquals("Should have 1 revisit record", 1, revisitCount);
    }

    @Test
    public void testNoDeduplicationWithoutCache() throws IOException {
        // Don't set cache size, so no deduplication should occur

        // Run deduplication
        dedupeTool.deduplicateWarcFile(testWarcFile, outputWarcFile);

        // Verify the output file exists
        assertTrue("Output file should exist", Files.exists(outputWarcFile));

        // Count the records in the output file and verify no deduplication
        int responseCount = 0;
        int revisitCount = 0;

        try (FileChannel channel = FileChannel.open(outputWarcFile, READ);
             WarcReader reader = new WarcReader(channel)) {

            for (WarcRecord record : reader) {
                if (record instanceof WarcResponse) {
                    responseCount++;
                } else if (record instanceof WarcRevisit) {
                    revisitCount++;
                }
            }
        }

        // We expect 3 response records and 0 revisit records (no deduplication)
        assertEquals("Should have 3 response records", 3, responseCount);
        assertEquals("Should have 0 revisit records", 0, revisitCount);
    }

    @Test
    public void testMinimumSizeThreshold() throws Exception {
        // Set cache size to enable deduplication
        dedupeTool.setCacheSize(10);

        // Set minimum size threshold higher than our test content
        dedupeTool.setMinimumSize(1000);

        // Run deduplication
        dedupeTool.deduplicateWarcFile(testWarcFile, outputWarcFile);

        // Verify the output file exists
        assertTrue("Output file should exist", Files.exists(outputWarcFile));

        // Count the records in the output file and verify no deduplication due to size threshold
        int responseCount = 0;
        int revisitCount = 0;

        try (FileChannel channel = FileChannel.open(outputWarcFile, READ);
             WarcReader reader = new WarcReader(channel)) {

            for (WarcRecord record : reader) {
                if (record instanceof WarcResponse) {
                    responseCount++;
                } else if (record instanceof WarcRevisit) {
                    revisitCount++;
                }
            }
        }

        // We expect 3 response records and 0 revisit records (no deduplication due to size threshold)
        assertEquals("Should have 3 response records", 3, responseCount);
        assertEquals("Should have 0 revisit records", 0, revisitCount);
    }

    @Test
    public void testDryRun() throws Exception {
        // Set cache size to enable deduplication
        dedupeTool.setCacheSize(10);

        // Set minimum size to a small value to allow deduplication of our test payloads
        dedupeTool.setMinimumSize(10);

        // Enable dry run mode
        dedupeTool.setDryRun(true);

        // Run dry run deduplication - this should not create any output file
        dedupeTool.deduplicateWarcFile(testWarcFile, null);
    }

    @Test
    public void testDetermineOutputPath() {
        // Test with various file extensions
        assertEquals("file-dedup.warc.gz", DedupeTool.determineOutputPath(Paths.get("file.warc.gz")).getFileName().toString());
        assertEquals("file-dedup.warc", DedupeTool.determineOutputPath(Paths.get("file.warc")).getFileName().toString());
        assertEquals("file-dedup.arc.gz", DedupeTool.determineOutputPath(Paths.get("file.arc.gz")).getFileName().toString());
        assertEquals("file-dedup.arc", DedupeTool.determineOutputPath(Paths.get("file.arc")).getFileName().toString());
        assertEquals("file.txt.dedup", DedupeTool.determineOutputPath(Paths.get("file.txt")).getFileName().toString());
    }
}
