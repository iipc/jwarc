/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.cdx;

import org.netpreserve.jwarc.WarcCaptureRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;

public class CdxFormat {
    public static final String CDX9_LEGEND = "N b a m s k r V g";
    public static final String CDX10_LEGEND = "N b a m s k r M V g";
    public static final String CDX11_LEGEND = "N b a m s k r M S V g";
    public static final CdxFormat CDX9 = new CdxFormat(CDX9_LEGEND);
    public static final CdxFormat CDX10 = new CdxFormat(CDX10_LEGEND);
    public static final CdxFormat CDX11 = new CdxFormat(CDX11_LEGEND);

    private final byte[] fieldNames;
    private final byte[] fieldIndices;
    private final boolean digestUnchanged;

    public CdxFormat(String legend) {
        this(legend, false);
    }

    private CdxFormat(String legend, boolean digestUnchanged) {
        this.digestUnchanged = digestUnchanged;
        String[] fields = legend.replaceFirst("^ ?CDX ", "").split(" ");
        fieldNames = new byte[fields.length];
        fieldIndices = new byte[128];
        Arrays.fill(fieldIndices, (byte) -1);
        for (byte i = 0; i < fields.length; i++) {
            if (fields[i].length() != 1) {
                throw new IllegalArgumentException("CDX field names must be a single ASCII character");
            }
            byte fieldName = (byte) fields[i].charAt(0);
            fieldNames[i] = fieldName;
            fieldIndices[fieldName] = i;
        }
    }

    int indexOf(int field) {
        if (field > fieldIndices.length) return -1;
        return fieldIndices[field];
    }

    public String legend() {
        StringBuilder builder = new StringBuilder();
        for (byte fieldName : fieldNames) {
            if (builder.length() > 0) builder.append(' ');
            builder.append((char) fieldName);
        }
        return builder.toString();
    }

    public String toString() {
        return "CdxFormat(\"" + legend() + "\")";
    }

    public CdxRecord parse(String line) {
        try {
            return new CdxRecord(line, this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public String format(WarcCaptureRecord record, String filename, long position, long size) {
        return format(record, filename, position, size, null);
    }

    public String format(WarcCaptureRecord record, String filename, long position, long size, String urlkey) {
        StringBuilder builder = new StringBuilder();
        for (byte fieldName : fieldNames) {
            if (builder.length() > 0) builder.append(' ');
            String value;
            switch (fieldName) {
                case CdxFields.FILENAME:
                    value = filename;
                    break;
                case CdxFields.COMPRESSED_ARC_FILE_OFFSET:
                    value = String.valueOf(position);
                    break;
                case CdxFields.COMPRESSED_RECORD_SIZE:
                    value = String.valueOf(size);
                    break;
                case CdxFields.NORMALIZED_SURT:
                    if (urlkey != null) {
                        value = urlkey;
                    } else {
                        value = CdxFields.format(fieldName, record,digestUnchanged);
                    }
                    break;
                default:
                    value = CdxFields.format(fieldName, record,digestUnchanged);
            }
            builder.append(value);
        }
        return builder.toString();
    }
    
    public static class Builder {
        private String legend;
        private boolean digestUnchanged = false;

        public Builder() {
            this.legend = CDX11_LEGEND;
        }

        public Builder legend(String legend) {
            this.legend = Objects.requireNonNull(legend);
            return this;
        }

        public Builder digestUnchanged() {
            digestUnchanged = true;
            return this;
        }

        public CdxFormat build() {
            return new CdxFormat(legend, digestUnchanged);
        }
    }
}
