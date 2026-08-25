/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2026 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.tools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;
import org.netpreserve.jwarc.WarcResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Iterator;

import static org.junit.Assert.*;

public class PackToolTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void packsFilesAsResourceRecords() throws Exception {
        Path html = temporaryFolder.newFile("file 1.html").toPath();
        Path unknown = temporaryFolder.newFile("file2.unknown-extension").toPath();
        Path warc = temporaryFolder.newFile("example.warc").toPath();
        Files.write(html, "<h1>Hello</h1>".getBytes(StandardCharsets.UTF_8));
        Files.write(unknown, new byte[]{1, 2, 3});
        Instant modified = Instant.parse("2024-03-02T01:02:03Z");
        Files.setLastModifiedTime(html, FileTime.from(modified));

        PackTool.main(new String[]{"-o", warc.toString(), "http://example.com/", html.toString(), unknown.toString()});

        try (WarcReader reader = new WarcReader(warc)) {
            Iterator<WarcRecord> records = reader.iterator();
            assertTrue(records.hasNext());
            WarcResource first = (WarcResource) records.next();
            assertEquals("http://example.com/file%201.html", first.target().toString());
            assertEquals(modified, first.date());
            assertEquals("text/html", first.contentType().toString());
            assertArrayEquals("<h1>Hello</h1>".getBytes(StandardCharsets.UTF_8),
                    readAllBytes(first.body().stream()));

            assertTrue(records.hasNext());
            WarcResource second = (WarcResource) records.next();
            assertEquals("application/octet-stream", second.contentType().toString());
            assertFalse(records.hasNext());
        }
    }

    private static byte[] readAllBytes(InputStream stream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = stream.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
