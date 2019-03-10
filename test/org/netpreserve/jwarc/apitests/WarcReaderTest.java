/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.apitests;

import org.junit.Test;
import org.netpreserve.jwarc.WarcFilter;
import org.netpreserve.jwarc.WarcReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class WarcReaderTest {
    @Test
    public void emptyFileShouldReturnNoRecords() throws IOException {
        WarcReader reader = new WarcReader(Channels.newChannel(new ByteArrayInputStream(new byte[0])));
        assertFalse(reader.iterator().hasNext());
        assertFalse(reader.next().isPresent());
        assertEquals(0, reader.records().count());
    }
}