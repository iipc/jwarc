/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

/**
 * Metadata about where a parsed record came from.
 */
class RecordSource {
    private final String filename;
    private final long offset;

    RecordSource(String filename, long offset) {
        this.filename = filename;
        this.offset = offset;
    }

    public String toString() {
        if (filename == null) {
            return "record offset " + offset;
        }
        return "record offset " + offset + " in " + filename;
    }
}
