/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;

public class WarcDigest {

    private final String algorithm;
    private final String value;

    public WarcDigest(String digest) {
        int i = digest.indexOf(':');
        if (i == -1) {
            throw new IllegalArgumentException("Invalid WARC-Digest");
        }
        this.algorithm = digest.substring(0, i);
        this.value = digest.substring(i + 1);
    }

    public WarcDigest(String algorithm, String value) {
        this.algorithm = algorithm;
        this.value = value;
    }

    public WarcDigest(String algorithm, byte[] value) {
        this(algorithm, base32Encode(value));
    }

    public WarcDigest(MessageDigest messageDigest) {
        algorithm = messageDigest.getAlgorithm().replace("-", "").toLowerCase(Locale.US);
        value = base32Encode(messageDigest.digest());
    }

    public String algorithm() {
        return algorithm;
    }

    public String base32() {
        return value;
    }

    public String hex() {
        return hexEncode(bytes());
    }

    public byte[] bytes() { return base32Decode(value); }

    @Override
    public String toString() {
        return prefixedBase32();
    }

    public String prefixedBase32() {
        return algorithm + ":" + value;
    }

    static String hexEncode(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for (byte b : data) {
            out.append("0123456789abcdef".charAt((b & 0xf0) >>> 4));
            out.append("0123456789abcdef".charAt(b & 0xf));
        }
        return out.toString();
    }

    static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder(data.length / 5 * 8);
        for (int i = 0; i < data.length;) {
            long bits = 0L;
            for (int j = 0; j < 5; j++) {
                bits = bits << 8 | data[i++] & 0xff;
            }
            for (int j = 0; j < 8; j++) {
                out.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".charAt((int) (bits >> 5 * (7 - j)) & 31));
            }
        }
        return out.toString();
    }

    static byte[] base32Decode(String data) {
        byte[] out = new byte[data.length() / 8 * 5];
        for (int i = 0, k = 0; k < out.length;) {
            long bits = 0L;
            for (int j = 0; j < 8; j++) {
                int c = data.charAt(i++) | 'A' ^ 'a';
                int value;
                if (c >= 'a' && c <= 'z') {
                    value = c - 'a';
                } else if (c >= '2' && c <= '7') {
                    value = c - '2' + 26;
                } else {
                    throw new IllegalArgumentException("Invalid base32 character: " + c);
                }
                bits = bits << 5 | value;
            }
            for (int j = 0; j < 5; j++) {
                out[k++] = (byte) ((bits >> 8 * (4 - j)) & 0xff);
            }
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarcDigest digest = (WarcDigest) o;
        return Objects.equals(algorithm, digest.algorithm) &&
                Objects.equals(value, digest.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, value);
    }
}
