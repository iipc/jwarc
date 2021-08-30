/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
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
        this.value = base32Encode(digest.substring(i + 1), this.algorithm);
    }

    public WarcDigest(String algorithm, String value) {
        this.algorithm = algorithm;
        this.value = base32Encode(value, algorithm);
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

    public String hex() {
        return hexEncode(bytes());
    }

    public String base16() {
        return hex();
    }

    public String base32() {
        return value;
    }

    public String base64() {
        return base64Encode(bytes());
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

    static byte[] hexDecode(String data) {
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

    static String base32Encode(byte[] data) {
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

    static byte[] base32Decode(String data) {
        int padding = 0;
        int outLength = data.length() / 8 * 5;
        if ((data.length() % 8) > 0) {
            // missing padding
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
                    // padding
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
            out = Arrays.copyOfRange(out, 0, (out.length - trim));
        }
        return out;
    }

    /** Decode the digest value if it's not Base32 */
    static String base32Encode(String value, String algorithm) {
        try {
            int length = getDigester(algorithm).getDigestLength();
            if ((length * 2) == value.length()) {
                // Base16
                return base32Encode(hexDecode(value));
            } else if ((length * 8 / 5) <= value.length()) {
                // Base32
            } else if ((length * 8 / 6) <= value.length()) {
                // Base64
                return base32Encode(Base64.getDecoder().decode(value));
            }
        } catch (NoSuchAlgorithmException e) {
            // ignore: no way to verify the digest anyway
        }
        return value;
    }

    static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data);
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

    /**
     * Get digester for algorithm names not matching the canonical Java names, e.g.
     * "sha256" instead of "SHA-256"
     */
    public static MessageDigest getDigester(String algorithm) throws NoSuchAlgorithmException {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            // transform "sha256" to "SHA-256" and similar
            if (algorithm.toLowerCase(Locale.ROOT).startsWith("sha")) {
                algorithm = "SHA-" + algorithm.substring(3);
            }
        }
        return MessageDigest.getInstance(algorithm);
    }

    public MessageDigest getDigester() throws NoSuchAlgorithmException {
        return getDigester(algorithm);
    }
}
