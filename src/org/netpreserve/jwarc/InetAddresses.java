/*
 * Safe InetAdress parsing borrowed from Guava. Just the methods we need, tweaked to remove dependencies.
 */


/*
 * Copyright (C) 2008 The Guava Authors
 * Copyright (C) 2024 National Library of Australia and the jwarc contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.netpreserve.jwarc;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Erik Kline
 */
final class InetAddresses {
    private static final int IPV4_PART_COUNT = 4;
    private static final int IPV6_PART_COUNT = 8;
    private static final Pattern IPV4_SPLITTER = Pattern.compile("\\.");
    private static final Pattern IPV6_SPLITTER = Pattern.compile(":");

    private InetAddresses() {
    }

    /**
     * Returns the {@link InetAddress} having the given string representation.
     *
     * <p>This deliberately avoids all nameservice lookups (e.g. no DNS).
     *
     * @param ipString {@code String} containing an IPv4 or IPv6 string literal, e.g. {@code
     *                 "192.168.0.1"} or {@code "2001:db8::1"}
     * @return {@link InetAddress} representing the argument
     * @throws IllegalArgumentException if the argument is not a valid IP string literal
     */
    public static InetAddress forString(String ipString) {
        byte[] addr = ipStringToBytes(ipString);

        // The argument was malformed, i.e. not an IP string literal.
        if (addr == null) {
            throw formatIllegalArgumentException("'%s' is not an IP string literal.", ipString);
        }

        return bytesToInetAddress(addr);
    }

    private static byte[] ipStringToBytes(String ipString) {
        // Make a first pass to categorize the characters in this string.
        boolean hasColon = false;
        boolean hasDot = false;
        for (int i = 0; i < ipString.length(); i++) {
            char c = ipString.charAt(i);
            if (c == '.') {
                hasDot = true;
            } else if (c == ':') {
                if (hasDot) {
                    return null; // Colons must not appear after dots.
                }
                hasColon = true;
            } else if (Character.digit(c, 16) == -1) {
                return null; // Everything else must be a decimal or hex digit.
            }
        }

        // Now decide which address family to parse.
        if (hasColon) {
            if (hasDot) {
                ipString = convertDottedQuadToHex(ipString);
                if (ipString == null) {
                    return null;
                }
            }
            return textToNumericFormatV6(ipString);
        } else if (hasDot) {
            return textToNumericFormatV4(ipString);
        }
        return null;
    }

    private static byte[] textToNumericFormatV4(String ipString) {
        byte[] bytes = new byte[IPV4_PART_COUNT];
        int i = 0;
        try {
            for (String octet : IPV4_SPLITTER.split(ipString, IPV4_PART_COUNT)) {
                bytes[i++] = parseOctet(octet);
            }
        } catch (NumberFormatException ex) {
            return null;
        }

        return i == IPV4_PART_COUNT ? bytes : null;
    }

    private static byte[] textToNumericFormatV6(String ipString) {
        // An address can have [2..8] colons, and N colons make N+1 parts.
        List<String> parts = Arrays.asList(IPV6_SPLITTER.split(ipString, IPV6_PART_COUNT + 2));
        if (parts.size() < 3 || parts.size() > IPV6_PART_COUNT + 1) {
            return null;
        }

        // Disregarding the endpoints, find "::" with nothing in between.
        // This indicates that a run of zeroes has been skipped.
        int skipIndex = -1;
        for (int i = 1; i < parts.size() - 1; i++) {
            if (parts.get(i).length() == 0) {
                if (skipIndex >= 0) {
                    return null; // Can't have more than one ::
                }
                skipIndex = i;
            }
        }

        int partsHi; // Number of parts to copy from above/before the "::"
        int partsLo; // Number of parts to copy from below/after the "::"
        if (skipIndex >= 0) {
            // If we found a "::", then check if it also covers the endpoints.
            partsHi = skipIndex;
            partsLo = parts.size() - skipIndex - 1;
            if (parts.get(0).length() == 0 && --partsHi != 0) {
                return null; // ^: requires ^::
            }
            if (parts.get(parts.size() - 1).length() == 0 && --partsLo != 0) {
                return null; // :$ requires ::$
            }
        } else {
            // Otherwise, allocate the entire address to partsHi. The endpoints
            // could still be empty, but parseHextet() will check for that.
            partsHi = parts.size();
            partsLo = 0;
        }

        // If we found a ::, then we must have skipped at least one part.
        // Otherwise, we must have exactly the right number of parts.
        int partsSkipped = IPV6_PART_COUNT - (partsHi + partsLo);
        if (!(skipIndex >= 0 ? partsSkipped >= 1 : partsSkipped == 0)) {
            return null;
        }

        // Now parse the hextets into a byte array.
        ByteBuffer rawBytes = ByteBuffer.allocate(2 * IPV6_PART_COUNT);
        try {
            for (int i = 0; i < partsHi; i++) {
                rawBytes.putShort(parseHextet(parts.get(i)));
            }
            for (int i = 0; i < partsSkipped; i++) {
                rawBytes.putShort((short) 0);
            }
            for (int i = partsLo; i > 0; i--) {
                rawBytes.putShort(parseHextet(parts.get(parts.size() - i)));
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return rawBytes.array();
    }

    private static String convertDottedQuadToHex(String ipString) {
        int lastColon = ipString.lastIndexOf(':');
        String initialPart = ipString.substring(0, lastColon + 1);
        String dottedQuad = ipString.substring(lastColon + 1);
        byte[] quad = textToNumericFormatV4(dottedQuad);
        if (quad == null) {
            return null;
        }
        String penultimate = Integer.toHexString(((quad[0] & 0xff) << 8) | (quad[1] & 0xff));
        String ultimate = Integer.toHexString(((quad[2] & 0xff) << 8) | (quad[3] & 0xff));
        return initialPart + penultimate + ":" + ultimate;
    }

    private static byte parseOctet(String ipPart) {
        // Note: we already verified that this string contains only hex digits.
        int octet = Integer.parseInt(ipPart);
        // Disallow leading zeroes, because no clear standard exists on
        // whether these should be interpreted as decimal or octal.
        if (octet > 255 || (ipPart.startsWith("0") && ipPart.length() > 1)) {
            throw new NumberFormatException();
        }
        return (byte) octet;
    }

    private static short parseHextet(String ipPart) {
        // Note: we already verified that this string contains only hex digits.
        int hextet = Integer.parseInt(ipPart, 16);
        if (hextet > 0xffff) {
            throw new NumberFormatException();
        }
        return (short) hextet;
    }

    /**
     * Convert a byte array into an InetAddress.
     *
     * <p>{@link InetAddress#getByAddress} is documented as throwing a checked exception "if IP
     * address is of illegal length." We replace it with an unchecked exception, for use by callers
     * who already know that addr is an array of length 4 or 16.
     *
     * @param addr the raw 4-byte or 16-byte IP address in big-endian order
     * @return an InetAddress object created from the raw IP address
     */
    private static InetAddress bytesToInetAddress(byte[] addr) {
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }
    }

    private static IllegalArgumentException formatIllegalArgumentException(
            String format, Object... args) {
        return new IllegalArgumentException(String.format(Locale.ROOT, format, args));
    }

    /**
     * Returns the string representation of an {@link InetAddress}.
     *
     * <p>For IPv4 addresses, this is identical to {@link InetAddress#getHostAddress()}, but for IPv6
     * addresses, the output follows <a href="http://tools.ietf.org/html/rfc5952">RFC 5952</a> section
     * 4. The main difference is that this method uses "::" for zero compression, while Java's version
     * uses the uncompressed form (except on Android, where the zero compression is also done). The
     * other difference is that this method outputs any scope ID in the format that it was provided at
     * creation time, while Android may always output it as an interface name, even if it was supplied
     * as a numeric ID.
     *
     * <p>This method uses hexadecimal for all IPv6 addresses, including IPv4-mapped IPv6 addresses
     * such as "::c000:201".
     *
     * @param ip {@link InetAddress} to be converted to an address string
     * @return {@code String} containing the text-formatted IP address
     * @since 10.0
     */
    public static String toAddrString(InetAddress ip) {
        Objects.requireNonNull(ip);
        if (ip instanceof Inet4Address) {
            // For IPv4, Java's formatting is good enough.
            // requireNonNull accommodates Android's @RecentlyNullable annotation on getHostAddress
            return Objects.requireNonNull(ip.getHostAddress());
        }
        byte[] bytes = ip.getAddress();
        int[] hextets = new int[IPV6_PART_COUNT];
        for (int i = 0; i < hextets.length; i++) {
            hextets[i] = ((bytes[2 * i] & 0xFF) << 8) | (bytes[2 * i + 1] & 0xFF);
        }
        compressLongestRunOfZeroes(hextets);

        return hextetsToIPv6String(hextets) + scopeWithDelimiter((Inet6Address) ip);
    }

    private static String scopeWithDelimiter(Inet6Address ip) {
        // getHostAddress on android sometimes maps the scope ID to an invalid interface name; if the
        // mapped interface isn't present, fallback to use the scope ID (which has no validation against
        // present interfaces)
        NetworkInterface scopedInterface = ip.getScopedInterface();
        if (scopedInterface != null) {
            return "%" + scopedInterface.getName();
        }
        int scope = ip.getScopeId();
        if (scope != 0) {
            return "%" + scope;
        }
        return "";
    }

    /**
     * Identify and mark the longest run of zeroes in an IPv6 address.
     *
     * <p>Only runs of two or more hextets are considered. In case of a tie, the leftmost run wins. If
     * a qualifying run is found, its hextets are replaced by the sentinel value -1.
     *
     * @param hextets {@code int[]} mutable array of eight 16-bit hextets
     */
    private static void compressLongestRunOfZeroes(int[] hextets) {
        int bestRunStart = -1;
        int bestRunLength = -1;
        int runStart = -1;
        for (int i = 0; i < hextets.length + 1; i++) {
            if (i < hextets.length && hextets[i] == 0) {
                if (runStart < 0) {
                    runStart = i;
                }
            } else if (runStart >= 0) {
                int runLength = i - runStart;
                if (runLength > bestRunLength) {
                    bestRunStart = runStart;
                    bestRunLength = runLength;
                }
                runStart = -1;
            }
        }
        if (bestRunLength >= 2) {
            Arrays.fill(hextets, bestRunStart, bestRunStart + bestRunLength, -1);
        }
    }

    /**
     * Convert a list of hextets into a human-readable IPv6 address.
     *
     * <p>In order for "::" compression to work, the input should contain negative sentinel values in
     * place of the elided zeroes.
     *
     * @param hextets {@code int[]} array of eight 16-bit hextets, or -1s
     */
    private static String hextetsToIPv6String(int[] hextets) {
        // While scanning the array, handle these state transitions:
        //   start->num => "num"     start->gap => "::"
        //   num->num   => ":num"    num->gap   => "::"
        //   gap->num   => "num"     gap->gap   => ""
        StringBuilder buf = new StringBuilder(39);
        boolean lastWasNumber = false;
        for (int i = 0; i < hextets.length; i++) {
            boolean thisIsNumber = hextets[i] >= 0;
            if (thisIsNumber) {
                if (lastWasNumber) {
                    buf.append(':');
                }
                buf.append(Integer.toHexString(hextets[i]));
            } else {
                if (i == 0 || lastWasNumber) {
                    buf.append("::");
                }
            }
            lastWasNumber = thisIsNumber;
        }
        return buf.toString();
    }
}
