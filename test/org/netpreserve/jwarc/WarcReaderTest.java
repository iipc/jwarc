/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.zip.DataFormatException;

import static org.junit.Assert.*;

public class WarcReaderTest {
    @Test
    public void test() throws IOException, DataFormatException {
        WarcReader reader = new WarcReader(Channels.newChannel(new ByteArrayInputStream(new byte[0])));
//        assertFalse(reader.iterator().hasNext());
    }

}