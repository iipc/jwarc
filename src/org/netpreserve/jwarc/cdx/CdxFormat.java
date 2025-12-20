/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2021 National Library of Australia
 */

package org.netpreserve.jwarc.cdx;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import static org.netpreserve.jwarc.cdx.CdxFields.*;

public class CdxFormat {
    public static final String CDX9_LEGEND = "N b a m s k r V g";
    public static final String CDX10_LEGEND = "N b a m s k r M V g";
    public static final String CDX11_LEGEND = "N b a m s k r M S V g";
    public static final CdxFormat CDX9 = new CdxFormat(CDX9_LEGEND);
    public static final CdxFormat CDX10 = new CdxFormat(CDX10_LEGEND);
    public static final CdxFormat CDX11 = new CdxFormat(CDX11_LEGEND);
    public static final CdxFormat CDXJ = new Builder().legend("N b")
            .json("url", "mime", "status", "digest", "length", "offset", "filename")
            .build();

    // PyWb has defined this fake Mime-type value to identify revisit
    public static final String PYWB_REVISIT_MIMETYPE = "warc/revisit";

    private final byte[] fieldNames;
    private final byte[] fieldIndices;
    private final String[] jsonFields;
    private final boolean digestUnchanged;

    public CdxFormat(String legend) {
        this(legend, false, null);
    }

    private CdxFormat(String legend, boolean digestUnchanged, String[] jsonFields) {
        this.digestUnchanged = digestUnchanged;
        this.jsonFields = jsonFields;
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
            try {
                value = formatField(fieldName, record, filename, position, size, urlkey);
            } catch (Exception e) {
                value = "-";
            }
            builder.append(value);
        }
        formatJsonBlock(record, filename, position, size, builder);
        return builder.toString();
    }

    private void formatJsonBlock(WarcCaptureRecord record, String filename, long position, long size, StringBuilder out) {
        if (jsonFields == null || jsonFields.length == 0) return;
        out.append(" {");
        boolean first = true;
        for (String field : jsonFields) {
            String value;
            switch (field) {
                case "digest":
                    value = record.payloadDigest().map(WarcDigest::raw).orElse(null);
                    break;
                case "filename":
                    value = filename;
                    break;
                case "length":
                    value = size < 0 ? null : String.valueOf(size);
                    break;
                case "mime":
                    if (record instanceof WarcRevisit) {
                        value = PYWB_REVISIT_MIMETYPE;
                    } else {
                        try {
                            value = record.payload().map(p -> p.type().base()).orElse(MediaType.OCTET_STREAM).toString();
                        } catch (IOException e) {
                            value = null;
                        }
                    }
                    break;
                case "offset":
                    value = String.valueOf(position);
                    break;
                case "status":
                    try {
                        value = String.valueOf(statusCode(record));
                    } catch (IOException e) {
                        value = null;
                    }
                    break;
                case "url":
                    value = record.target();
                    break;
                default:
                    value = null;
            }
            if (value == null) continue;

            if (!first) out.append(", ");
            first = false;
            out.append('"');
            out.append(field);
            out.append("\": \"");
            escapeJsonString(out, value);
            out.append('"');
        }
        out.append("}");
    }

    private static void escapeJsonString(StringBuilder out, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') out.append("\\\"");
            else if (c == '\\') out.append("\\\\");
            else if (c == '\b') out.append("\\b");
            else if (c == '\f') out.append("\\f");
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c <= 0x1f) {
                out.append("\\u00");
                out.append(Character.forDigit((c & 0xf0) >>> 4, 16));
                out.append(Character.forDigit(c & 0xf, 16));
            } else {
                out.append(c);
            }
        }
    }

    private static int statusCode(WarcCaptureRecord record) throws IOException {
        if (record instanceof WarcResponse || record instanceof WarcRevisit) {
            if (record instanceof WarcRevisit) {
                return ((WarcRevisit) record).http().status();
            } else if (record.contentType().base().equals(MediaType.HTTP)) {
                return ((WarcResponse) record).http().status();
            } else if (record.contentType().base().equals(MediaType.GEMINI)) {
                return ((WarcResponse) record).gemini().statusHttpEquivalent();
            }
        }
        return 200;
    }

    String formatField(byte fieldName, WarcCaptureRecord record, String filename, long position, long size, String urlkey) throws IOException {
        switch (fieldName) {
            case CHECKSUM:
                return record.payloadDigest()
                        .map(digestUnchanged ? WarcDigest::raw : WarcDigest::base32)
                        .map(CdxFormat::escape)
                        .orElse("-");
            case COMPRESSED_ARC_FILE_OFFSET:
                return position < 0 ? "-" : String.valueOf(position);
            case COMPRESSED_RECORD_SIZE:
                return size < 0 ? "-" : String.valueOf(size);
            case DATE:
                return CdxFields.DATE_FORMAT.format(record.date());
            case FILENAME:
                return filename == null ? "-" : escape(filename);
            case MIME_TYPE:
                if (record instanceof WarcRevisit) {
                    return PYWB_REVISIT_MIMETYPE;
                } else {
                    return escape(record.payload().map(p -> p.type().base()).orElse(MediaType.OCTET_STREAM).toString());
                }
            case NORMALIZED_SURT:
                if (urlkey != null) {
                    return escape(urlkey);
                } else {
                    return escape(URIs.toNormalizedSurt(record.target()));
                }
            case ORIGINAL_URL:
                return escape(record.target());
            case REDIRECT:
                if (record instanceof WarcResponse) {
                    return ((WarcResponse) record).http().headers().first("Location").map(CdxFormat::escape).orElse("-");
                } else {
                    return "-";
                }
            case RESPONSE_CODE:
                if (record instanceof WarcResponse || record instanceof WarcRevisit) {
                    if (record instanceof WarcRevisit) {
                        return Integer.toString(((WarcRevisit) record).http().status());
                    }
                    else if (record.contentType().base().equals(MediaType.HTTP)) {
                        return Integer.toString(((WarcResponse) record).http().status());
                    } else if (record.contentType().base().equals(MediaType.GEMINI)) {
                        return String.format("%02d", ((WarcResponse) record).gemini().statusHttpEquivalent());
                    }
                }
                return Integer.toString(statusCode(record));
            default:
                throw new IllegalArgumentException("Unknown CDX field: " + (char) fieldName);
        }
    }

    private static String escape(String str) {
        if (str == null) return null;
        return str.replace(" ", "%20")
                .replace("\n", "%0A")
                .replace("\0", "%00");
    }

    public static class Builder {
        private String legend;
        private boolean digestUnchanged = false;
        private String[] jsonFields;

        public Builder() {
            this.legend = CDX11_LEGEND;
        }

        public Builder(CdxFormat base) {
            legend = base.legend();
            digestUnchanged = base.digestUnchanged;
            jsonFields = base.jsonFields;
        }

        public Builder legend(String legend) {
            this.legend = Objects.requireNonNull(legend);
            return this;
        }

        public Builder digestUnchanged() {
            digestUnchanged = true;
            return this;
        }

        private Builder json(String... jsonFields) {
            this.jsonFields = jsonFields;
            return this;
        }

        public CdxFormat build() {
            return new CdxFormat(legend, digestUnchanged, jsonFields);
        }
    }
}
