/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2020 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.nio.ByteBuffer;

public class MessageParser {

    protected static String getErrorContext(String input, int position, int length) {
        StringBuilder context = new StringBuilder();

        int start = position - length;
        if (start < 0) {
            start = 0;
        } else {
            context.append("...");
        }
        int end = Math.min(input.length(), (position + length));

        context.append(input.substring(start, position));
        context.append("<-- HERE -->");
        context.append(input.substring(position, end));

        if (end < input.length()) {
            context.append("...");
        }

        return context.toString();
    }

    protected static String getErrorContext(ByteBuffer buffer, int position, int length) {
        StringBuilder context = new StringBuilder();

        int start = position - length;
        if (start < 0) {
            start = 0;
        } else {
            context.append("...");
        }

        ByteBuffer copy = buffer.duplicate();
        copy.position(start);

        int end = position + length;
        if (end < buffer.limit()) {
            copy.limit(end);
        }

        while (true) {
            if (copy.position() == position) {
                context.append("<-- HERE -->");
            }
            if (!copy.hasRemaining()) break;
            int c = (int) copy.get();
            if (c < 0x7f && c >= 0x20) {
                context.append((char) c);
            } else if (c == 0x09) {
                context.append("\\t");
            } else if (c == 0x0a) {
                context.append("\\n");
            } else if (c == 0x0d) {
                context.append("\\r");
            } else {
                context.append(String.format("\\x%02x", c));
            }
        }

        if (copy.position() < buffer.limit()) {
            context.append("...");
        }

        return context.toString();
    }

}
