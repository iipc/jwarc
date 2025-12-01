/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.tools.DigestEncodingUtils;
import java.security.MessageDigest;

import java.util.Locale;
import java.util.Objects;

import static org.netpreserve.jwarc.tools.DigestEncodingUtils.*;

public class WarcDigest {
    private final String raw;
    private String algorithm;
    private String value;

    public WarcDigest(String digest) {
        Objects.requireNonNull(digest);
        raw = digest;
    }

    public WarcDigest(String algorithm, String value) {
        Objects.requireNonNull(algorithm);
        Objects.requireNonNull(value);
        raw = algorithm + ":" + value;
        this.algorithm = canonicalizeAlgorithm(algorithm);
        this.value = base32Encode(value, algorithm);
    }

    public WarcDigest(String algorithm, byte[] value) {
        this(algorithm, base32Encode(value));
    }

    public WarcDigest(MessageDigest messageDigest) {
        algorithm = canonicalizeAlgorithm(messageDigest.getAlgorithm());
        value = base32Encode(messageDigest.digest());
        raw = algorithm + ":" + value;
    }

    private static String canonicalizeAlgorithm(String algorithm) {
        return algorithm.replace("-", "").toLowerCase(Locale.US);
    }

    private void parse() {
        if (value != null) return;
        int i = raw.indexOf(':');
        if (i == -1) {
            throw new IllegalArgumentException("Invalid WARC-Digest");
        }
        this.algorithm = canonicalizeAlgorithm(raw.substring(0, i));
        this.value = base32Encode(raw.substring(i + 1), this.algorithm);
    }

    public String algorithm() {
        parse();
        return algorithm;
    }

    private String value() {
        parse();
        return value;
    }


    public String base16() {
        return hex();
    }
    public String hex() {
        return DigestEncodingUtils.hexEncode(bytes());
    }
    public String base32() {
        return value();
    }

    public String base64() {
        return base64Encode(bytes());
    }

    public byte[] bytes() { return base32Decode(value()); }

    /**
     * Returns the original digest string without any canonicalization.
     */
    public String raw() {
        return raw;
    }

    @Override
    public String toString() {
        return prefixedBase32();
    }

    public String prefixedBase32() {
        return algorithm() + ":" + value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarcDigest digest = (WarcDigest) o;
        return Objects.equals(algorithm(), digest.algorithm()) &&
                Objects.equals(value(), digest.value());
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm(), value());
    }
}
