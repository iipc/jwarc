/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.FormatDetectingParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.zip.DataFormatException;

public class WarcReader implements Iterable<WarcRecord>, Iterator<WarcRecord> {
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
    private FormatDetectingParser parser;
    public WarcReader(ReadableByteChannel channel) throws IOException, DataFormatException {
        this.channel = channel;
        channel.read(buffer);
        parser.update(buffer);
    }

    @Override
    public Iterator<WarcRecord> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public WarcRecord next() {
        return null;
    }
}
