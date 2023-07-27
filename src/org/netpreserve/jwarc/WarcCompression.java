/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.nio.file.Path;

public enum WarcCompression {
    NONE, GZIP;

    static WarcCompression forPath(Path path) {
        if (path.getFileName().toString().endsWith(".gz")) {
            return GZIP;
        } else {
            return NONE;
        }
    }
}