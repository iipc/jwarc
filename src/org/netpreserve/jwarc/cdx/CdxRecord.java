/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.cdx;

import org.netpreserve.jwarc.MediaType;
import org.netpreserve.jwarc.URIs;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

public class CdxRecord {
    private final String[] values;
    private final CdxFormat format;

    CdxRecord(String line, CdxFormat format) throws IOException {
        this.values = line.split(" ");
        if (format != null) {
            this.format = format;
        } else if (values.length == 9) {
            this.format = CdxFormat.CDX9;
        } else if (values.length == 10) {
            this.format = CdxFormat.CDX10;
        } else if (values.length == 11) {
            this.format = CdxFormat.CDX11;
        } else {
            throw new IOException("Unable to determine the CDX format");
        }
    }

    public String get(int field) {
        int i = format.indexOf(field);
        if (i == -1) return null;
        String value = values[i];
        return value.equals("-") ? null : value;
    }

    public Instant date() {
        String value = get(CdxFields.DATE);
        return value == null ? null : CdxFields.DATE_FORMAT.parse(value, Instant::from);
    }

    public String filename() {
        return get(CdxFields.FILENAME);
    }

    public String target() {
        return get(CdxFields.ORIGINAL_URL);
    }

    public URI targetURI() {
        String value = target();
        return value == null ? null : URIs.parseLeniently(value);
    }

    /**
     * Length of the WARC record in bytes. Including headers and measured after any compression is applied.
     */
    public Long size() {
        String value = get(CdxFields.COMPRESSED_RECORD_SIZE);
        return value == null ? null : Long.parseLong(value);
    }

    /**
     * Position in bytes of the record in the WARC file.
     */
    public Long position() {
        String value = get(CdxFields.COMPRESSED_ARC_FILE_OFFSET);
        return value == null ? null : Long.parseLong(value);
    }

    /**
     * HTTP response status code.
     */
    public Integer status() {
        String value = get(CdxFields.RESPONSE_CODE);
        return value == null ? null : Integer.parseInt(value);
    }

    /**
     * A cryptographic digest of the response payload. Most commonly this is a SHA-1 digest in base 32 or an MD5 digest
     * in hexadecimal.
     */
    public String digest() {
        return get(CdxFields.CHECKSUM);
    }

    /**
     * The value of the Location HTTP header for redirect responses.
     */
    public String redirect() {
        return get(CdxFields.REDIRECT);
    }

    public MediaType contentType() {
        String value = get(CdxFields.MIME_TYPE);
        return value == null ? null : MediaType.parseLeniently(value);
    }
}
