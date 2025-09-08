/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;

interface DecompressingChannel {

    /**
     * Number of bytes read from the underlying channel.
     */
    long inputPosition();

    /**
     * Reset the decompressor, discarding any buffered data.
     */
    void reset() throws IOException;
}
