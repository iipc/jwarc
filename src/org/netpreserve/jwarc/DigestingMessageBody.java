/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * Wrapper around a MessageBody which calculates a MessageDigest while the body
 * is read.
 */
public class DigestingMessageBody extends MessageBody {

    private final MessageBody body;
    private final MessageDigest digest;
    private long length = 0;

    DigestingMessageBody(MessageBody digestedBody, MessageDigest digest) {
        this.body = digestedBody;
        this.digest = digest;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int i = body.read(dst);
        if (i > 0) {
            length += i;
            ByteBuffer tmp = dst.duplicate();
            tmp.position(dst.position() - i);
            tmp.limit(dst.position());
            digest.update(tmp);
        }
        return i;
    }

    @Override
    public boolean isOpen() {
        return body.isOpen();
    }

    @Override
    public void close() throws IOException {
        body.close();
    }

    @Override
    public long position() throws IOException {
        return body.position();
    }

    public MessageDigest getDigest() {
        return digest;
    }

    public long getLength() {
        return length;
    }
}