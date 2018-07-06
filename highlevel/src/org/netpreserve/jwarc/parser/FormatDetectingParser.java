/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.parser;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class FormatDetectingParser {
    private State state = State.START;
    private GzipHeaderParser gzipHeaderParser;
    private WarcParser warcParser;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private Inflater inflater;

//    public FormatDetectingParser(WarcHandler handler) {
//        warcParser = new WarcParser(handler);
//    }

    public void update(ByteBuffer data) throws DataFormatException {
        if (inflater == null) {
            update1(data);
            return;
        }

        inflater.setInput(data.array(), data.arrayOffset() + data.position(), data.remaining());
        while (!inflater.needsInput()) {
            byte[] buf = new byte[512];
            int n = inflater.inflate(buf);
            update1(ByteBuffer.wrap(buf, 0, n));
        }
    }

    public void update1(ByteBuffer data) {
        switch (state) {
            case START:
                byte peek = data.get(buffer.position());
                if (peek == 31) {
                    gzipHeaderParser = new GzipHeaderParser();
                    state = State.GZIP_HEADER;
                } else if (peek == 'W') {
                    state = State.WARC_HEADER;
                } else {
                    state = State.ARC_HEADER;
                }
                break;

            case GZIP_HEADER:
                gzipHeaderParser.update(data);
                if (gzipHeaderParser.isDone()) {
                    gzipHeaderParser = null; // we don't actually need it
                    inflater = new Inflater();
                    state = State.START;
                }
                break;

            case WARC_HEADER:
                warcParser.parse(data);
                if (warcParser.isFinished()) {

                }

        }
    }

    private enum State {
        START, GZIP_HEADER, WARC_HEADER, ARC_HEADER
    }
}
