/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.cdx;

import org.netpreserve.jwarc.WarcCaptureRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

public class CdxFormat {
    public static final CdxFormat CDX9 = new CdxFormat("N b a m s k r V g");
    public static final CdxFormat CDX10 = new CdxFormat("N b a m s k r M V g");
    public static final CdxFormat CDX11 = new CdxFormat("N b a m s k r M S V g");

    private final byte[] fieldNames;
    private final byte[] fieldIndices;

    public CdxFormat(String legend) {
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
                default:
                    value = CdxFields.format(fieldName, record);
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
