package org.netpreserve.jwarc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class DigestUtils {

    private DigestUtils() {}

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

}
