/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.zip.DataFormatException;

public class WarcReader {
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
    private final static int CRLFCRLF = 0x0d0a0d0a;
    private WarcRecord record;

    public WarcReader(ReadableByteChannel channel) throws IOException, DataFormatException {
        this.channel = channel;
        buffer.flip();
    }

    public WarcRecord next() throws IOException {
        if (record != null) {
            consume(record.body());
            while (buffer.remaining() < 4) {
                buffer.compact();
                if (channel.read(buffer) < 0) throw new EOFException("missing trailer");
                buffer.flip();
            }
            int trailer = buffer.getInt();
            if (trailer != CRLFCRLF) { // CRLFCRLF
                throw new ParsingException("invalid trailer: " + Integer.toHexString(trailer));
            }
        }
        record = WarcRecord.parse(channel, buffer);
        return record;
    }

    private void consume(ReadableByteChannel channel) throws IOException {
        ByteBuffer tmp = ByteBuffer.allocate(8192);
        while (channel.read(tmp) >= 0) {
            tmp.clear();
        }
    }

    public static void main(String args[]) throws IOException, DataFormatException {
        while (true) {
            long start = System.currentTimeMillis();
            try (FileChannel channel = FileChannel.open(Paths.get("/tmp/her.warc"))) {
                WarcReader reader = new WarcReader(channel);
                while (true) {
                    WarcRecord record = reader.next();
                    if (record == null) break;
//                    System.out.println(record + " " + record.body().length());
                }
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }
}
