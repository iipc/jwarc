/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia
 */

package org.netpreserve.jwarc.tools;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import static org.netpreserve.jwarc.DigestUtils.getDigester;

public class DigestEncodingUtils {

    public static String hexEncode(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for (byte b : data) {
            out.append("0123456789abcdef".charAt((b & 0xf0) >>> 4));
            out.append("0123456789abcdef".charAt(b & 0xf));
        }
        return out.toString();
    }

    public static byte[] hexDecode(String data) {
        if ((data.length() % 2) == 1) {
            throw new IllegalArgumentException(
                    "Length of hex-encoded string (Base16) must be a multiple of 2, but is " + data.length());
        }
        byte[] out = new byte[data.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int v = Character.digit(data.charAt(i * 2), 16) << 4;
            v += Character.digit(data.charAt(i * 2 + 1), 16);
            out[i] = (byte) v;
        }
        return out;
    }

    public static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder(data.length / 5 * 8);
        int leftover = 0;
        long bits = 0L;
        for (int i = 0; i < data.length;) {
            bits = 0L;
            for (int j = 0; j < 5; j++) {
                if (i >= data.length) {
                    bits = bits << 8 | 0x00;
                    leftover++;
                } else {
                    bits = bits << 8 | data[i++] & 0xff;
                }
            }
            if (leftover > 0) break;
            for (int j = 0; j < 8; j++) {
                out.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".charAt((int) (bits >> 5 * (7 - j)) & 31));
            }
        }
        if (leftover > 0) {
            int trailing = 7 - ((5 - leftover) * 8 / 5);
            for (int j = 0; j < (8 - trailing); j++) {
                out.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".charAt((int) (bits >> 5 * (7 - j)) & 31));
            }
            for (int j = 0; j < trailing; j++) {
                out.append('=');
            }
        }
        return out.toString();
    }

    public static byte[] base32Decode(String data) {
        int padding = 0;
        int outLength = data.length() / 8 * 5;
        if ((data.length() % 8) > 0) {
            outLength += 5;
        }
        byte[] out = new byte[outLength];
        for (int i = 0, k = 0; k < out.length;) {
            long bits = 0L;
            for (int j = 0; j < 8; j++) {
                int c = '=';
                if (i < data.length())
                    c = data.charAt(i++) | 'A' ^ 'a';
                int value;
                if (c >= 'a' && c <= 'z') {
                    value = c - 'a';
                } else if (c >= '2' && c <= '7') {
                    value = c - '2' + 26;
                } else if (c == '=') {
                    value = 0;
                    padding++;
                } else {
                    throw new IllegalArgumentException(
                            "Invalid base32 character: " + c + " '" + (char) c + "'");
                }
                bits = bits << 5 | value;
            }
            for (int j = 0; j < 5; j++) {
                out[k++] = (byte) ((bits >> 8 * (4 - j)) & 0xff);
            }
        }
        if (padding > 0) {
            int trim = 5 - (43 - 5 * padding) / 8;
            out = Arrays.copyOfRange(out, 0, out.length - trim);
        }
        return out;
    }

    /** Decode the digest value if it's not Base32 */
    public static String base32Encode(String value, String algorithm) {
        try {
            int length = getDigester(algorithm).getDigestLength();
            if ((length * 2) == value.length()) {
                return base32Encode(hexDecode(value));
            } else if ((length * 8 / 5) <= value.length()) {
                // Already base32
            } else if ((length * 8 / 6) <= value.length()) {
                return base32Encode(Base64.getDecoder().decode(value));
            }
        } catch (NoSuchAlgorithmException e) {
        }
        return value;
    }

    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data);
    }
}
