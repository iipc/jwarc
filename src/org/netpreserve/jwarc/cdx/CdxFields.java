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

    private static String escape(String str) {
        return str == null ? null : str.replace(" ", "%20");
    }
    
    public static String format(byte field, WarcCaptureRecord record) {
        return format(field,record,false);
    }
      
    public static String format(byte field, WarcCaptureRecord record, boolean digestUnchanged) {
        try {
            switch (field) {
                case CHECKSUM:
                    return record.payloadDigest().map(digestUnchanged ? WarcDigest::raw : WarcDigest::base32)
                            .orElse("-");
                case DATE:
                    return DATE_FORMAT.format(record.date());
                case MIME_TYPE:
                    return escape(record.payload().map(p -> p.type().base()).orElse(MediaType.OCTET_STREAM).toString());
                case ORIGINAL_URL:
                    return escape(record.target());
                case NORMALIZED_SURT:
                    return escape(URIs.toNormalizedSurt(record.target()));
                case REDIRECT:
                    if (record instanceof WarcResponse) {
                        return ((WarcResponse)record).http().headers().first("Location").map(CdxFields::escape).orElse("-");
                    }
                    break;
                case RESPONSE_CODE:
                    if (record instanceof WarcResponse) {
                        if (record.contentType().base().equals(MediaType.HTTP)) {
                            return Integer.toString(((WarcResponse) record).http().status());
                        } else if (record.contentType().base().equals(MediaType.GEMINI)) {
                            return String.format("%02d", ((WarcResponse) record).gemini().statusHttpEquivalent());
                        }
                    }
                    return "200";
                default:
            }
        } catch (Exception e) {
            // ignore parse errors
        }
        return "-";
    }

}
