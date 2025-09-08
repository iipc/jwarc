/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018-2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.nio.file.Path;

public enum WarcCompression {
    NONE, GZIP, ZSTD;

    static WarcCompression forPath(Path path) {
        String filename = path.getFileName().toString();
        if (filename.endsWith(".gz")) {
            return GZIP;
        } else if (filename.endsWith(".zst")) {
            return ZSTD;
        } else {
            return NONE;
        }
    }
}