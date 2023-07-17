/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.cdx;

import org.netpreserve.jwarc.*;

import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

public final class CdxFields {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(UTC);

    private CdxFields() {
    }

    public static final byte ORIGINAL_URL = 'a';
    public static final byte DATE = 'b';
    public static final byte CHECKSUM = 'k';
    public static final byte FILENAME = 'g';
    public static final byte MIME_TYPE = 'm';
    public static final byte REDIRECT = 'r';
    public static final byte RESPONSE_CODE = 's';
    public static final byte NORMALIZED_SURT = 'N';
    public static final byte COMPRESSED_RECORD_SIZE = 'S';
    public static final byte COMPRESSED_ARC_FILE_OFFSET = 'V';

    public static String format(byte field, WarcCaptureRecord record) {
        try {
            return CdxFormat.CDX11.formatField(field, record, null , -1, -1, null);
        } catch (Exception e) {
            return "-";
        }
    }
}
