/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2024 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.brotli.dec.BrotliInputStream;

/**
 * Utility class to read brotli-encoded data, based on org.brotli:dec.
 */
public final class BrotliUtils {

    public static ReadableByteChannel brotliChannel(ReadableByteChannel brotli) throws IOException {
        return Channels.newChannel(new BrotliInputStream(Channels.newInputStream(brotli)));
    }
}
