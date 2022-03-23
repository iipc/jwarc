/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.cdx;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CdxReader implements Iterable<CdxRecord>, Closeable {
    private final BufferedReader reader;
    private CdxFormat format;

    public CdxReader(BufferedReader reader) {
        this.reader = reader;
    }

    public Optional<CdxRecord> next() throws IOException {
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // ignore comments
            }
            if (line.startsWith("CDX ")) {
                format = new CdxFormat(line);
                continue;
            }
            return Optional.of(new CdxRecord(line, format));
        }
        return Optional.empty();
    }

    @Override
    public Iterator<CdxRecord> iterator() {
        return new Iterator<CdxRecord>() {
            private CdxRecord next;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    try {
                        next = CdxReader.this.next().orElse(null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return next != null;
            }

            @Override
            public CdxRecord next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return next;
            }
        };
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
