/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.testsuite;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommonsReader implements TestReader {
    @Override
    public TestRecord read(InputStream stream) throws IOException {
        try (ArchiveReader reader = WARCReaderFactory.get("test.warc", stream, true)) {
            ArchiveRecord record = reader.iterator().next();
            return new TestRecord() {
                @Override
                public String getHeader(String name) {
                    return record.getHeader().getHeaderValue(name).toString();
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String,String> headers = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : record.getHeader().getHeaderFields().entrySet()) {
                        headers.put(entry.getKey(), entry.getValue().toString());
                    }
                    return headers;
                }
            };
        }
    }
}
