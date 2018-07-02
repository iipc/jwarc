/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.testsuite;

import org.jwat.common.HeaderLine;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class JWatReader implements TestReader {
    public TestRecord read(InputStream stream) throws IOException {
        try (WarcReader reader = WarcReaderFactory.getReaderUncompressed(stream)) {
            WarcRecord record = reader.getNextRecord();
            return new TestRecord() {
                @Override
                public String getHeader(String name) {
                    return record.getHeader(name).value;
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String,String> headers = new LinkedHashMap<>();
                    for (HeaderLine line : record.getHeaderList()) {
                        headers.put(line.name, line.value);
                    }
                    return headers;
                }
            };
        }
    }
}
