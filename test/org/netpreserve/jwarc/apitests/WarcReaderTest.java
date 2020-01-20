/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Test;
import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        URL warcFile = getClass().getClassLoader().getResource("org/netpreserve/jwarc/" + warcFileName);
        assertNotNull("WARC file " + warcFileName + " not found", warcFile);
        byte[] gzipped = Files.readAllBytes(Paths.get(warcFile.toURI()));

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

}