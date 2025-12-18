/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reader for Web Archive Collection Zipped (WACZ) files.
 *
 * @see <a href="https://specs.webrecorder.net/wacz/latest/">WACZ Specification</a>
 */
public class WaczReader implements Closeable {
    private final ZipFile zip;
    private Map<String, Object> metadata;

    public WaczReader(Path path) throws IOException {
        this.zip = new ZipFile(path.toFile());
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    /**
     * Retrieves the metadata from the WACZ file, reading it from "datapackage.json".
     * Caches the result.
     *
     * @return a Map representing the metadata contained in "datapackage.json".
     * @throws IOException if the WACZ file does not contain "datapackage.json",
     *                     if the file cannot be read,
     *                     or if "datapackage.json" is not a valid JSON object.
     */
    public Map<String, Object> metadata() throws IOException {
        if (metadata != null) return metadata;
        ZipEntry entry = zip.getEntry("datapackage.json");
        if (entry == null) throw new IOException("WACZ file is missing datapackage.json");
        try (InputStream stream = zip.getInputStream(entry)) {
            Object value = Json.read(stream);
            if (!(value instanceof Map)) throw new IOException("datapackage.json is not a JSON object");
            //noinspection unchecked
            this.metadata = (Map<String, Object>) value;
            return metadata;
        }
    }
}
