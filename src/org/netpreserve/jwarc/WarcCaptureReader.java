/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia
 */

package org.netpreserve.jwarc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A WARC file reader which groups a sequence of concurrent records into capture events.
 * <p>
 * EXPERIMENTAL API: May change or be removed without notice.
 */
public class WarcCaptureReader implements Closeable {
    private final WarcReader reader;
    private WarcCaptureRecord nextRecord;
    private Warcinfo warcinfo;

    public WarcCaptureReader(WarcReader reader) {
        this.reader = reader;
    }

    public WarcCaptureReader(ReadableByteChannel channel) throws IOException {
        this(new WarcReader(channel));
    }

    public WarcCaptureReader(InputStream stream) throws IOException {
        this(new WarcReader(stream));
    }

    public WarcCaptureReader(Path path) throws IOException {
        this(new WarcReader(path));
    }

    private WarcCaptureRecord peek() throws IOException {
        while (nextRecord == null) {
            WarcRecord record = reader.next().orElse(null);
            if (record == null) return null;
            if (record instanceof WarcCaptureRecord) {
                nextRecord = (WarcCaptureRecord) record;
            } else if (record instanceof Warcinfo) {
                warcinfo = (Warcinfo) record;
            }
        }
        return nextRecord;
    }

    private WarcCapture read() throws IOException {
        WarcCaptureRecord record;
        ConcurrentRecordSet concurrentSet = new ConcurrentRecordSet();
        List<WarcCaptureRecord> records = new ArrayList<>();
        // read until we find a main record (response, resource or revisit)
        do {
            record = peek();
            if (record == null) return null;
            if (!concurrentSet.contains(record)) {
                concurrentSet.clear();
                concurrentSet.add(record);
                records.clear();
            }
            records.add(record);
            // TODO: buffer secondary records
            nextRecord = null;
        } while (!(record instanceof WarcResponse || record instanceof WarcResource || record instanceof WarcRevisit));
        return new WarcCapture(this, concurrentSet, record, records, warcinfo);
    }

    public Optional<WarcCapture> next() throws IOException {
        return Optional.ofNullable(read());
    }

    WarcCaptureRecord readConcurrentTo(ConcurrentRecordSet concurrentSet) throws IOException {
        WarcCaptureRecord record = peek();
        if (record != null && concurrentSet.contains(record)) {
            nextRecord = null;
            return record;
        }
        return null;
    }

    /**
     * Seeks to the record at the given position in the underlying channel.
     *
     * @param position byte offset of the beginning of the record to seek to
     * @throws IOException                   if an I/O error occurs
     * @throws IllegalArgumentException      if the position is negative
     * @throws UnsupportedOperationException if the underlying channel does not support seeking
     */
    public void position(long position) throws IOException {
        reader.position(position);
        nextRecord = null;
        warcinfo = null;
    }

    /**
     * Closes the underlying WarcReader.
     * @throws IOException if an I/O error occurs during the close operation.
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
