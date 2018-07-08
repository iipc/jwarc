/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.nio.ByteBuffer;

public class GzipHeaderParser {
    private static final int FHCRC = 2;
    private static final int FEXTRA = 4;
    private static final int FNAME = 8;
    private static final int FCOMMENT = 16;

    private State state = State.FIXEDLEN;
    private final ByteBuffer header = ByteBuffer.allocate(10);
    private ByteBuffer extra;
    private StringBuilder filename;
    private StringBuilder comment;
    private int xlen;
    private int hcrc;

    public void update(ByteBuffer data) {
        while (data.hasRemaining()) {
            byte b;
            switch (state) {
                case FIXEDLEN:
                    header.put((ByteBuffer) data.slice().limit(header.remaining()));
                    if (!header.hasRemaining()) {
                        state = nextState();
                    }
                    break;
                case XLEN1:
                    xlen = data.get();
                    state = State.XLEN2;
                    break;
                case XLEN2:
                    xlen |= data.get() << 8;
                    extra = ByteBuffer.allocate(xlen);
                    state = State.EXTRA;
                    break;
                case EXTRA:
                    extra.put((ByteBuffer) data.slice().limit(extra.remaining()));
                    if (!extra.hasRemaining()) {
                        state = nextState();
                    }
                    break;
                case NAME:
                    b = data.get();
                    if (b != 0) {
                        filename.append((char) b);
                    } else {
                        state = nextState();
                    }
                    break;
                case COMMENT:
                    b = data.get();
                    if (b != 0) {
                        comment.append((char) b);
                    } else {
                        state = nextState();
                    }
                    break;
                case HCRC1:
                    hcrc = data.get();
                    state = State.HCRC2;
                    break;
                case HCRC2:
                    hcrc |= data.get() << 8;
                    state = State.DONE;
                    break;
                case DONE:
                    return;
            }
        }
    }

    private State nextState() {
        int flag = header.get(3);
        switch (state) {
            case FIXEDLEN:
                if ((flag & FEXTRA) != 0) return State.XLEN1;
            case EXTRA:
                if ((flag & FNAME) != 0) return State.NAME;
            case NAME:
                if ((flag & FCOMMENT) != 0) {
                    filename = new StringBuilder();
                    return State.COMMENT;
                }
            case COMMENT:
                if ((flag & FHCRC) != 0) {
                    comment = new StringBuilder();
                    return State.HCRC1;
                }
                return State.DONE;
            default:
                throw new IllegalStateException(state.name());
        }

    }

    public boolean isDone() {
        return state == State.DONE;
    }

    enum State {
        FIXEDLEN, XLEN1, XLEN2, EXTRA, NAME, COMMENT, HCRC1, HCRC2, DONE;
    }
}
