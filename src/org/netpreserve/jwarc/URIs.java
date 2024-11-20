package org.netpreserve.jwarc;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.DOTALL;

public class URIs {
    private final static Pattern URL_REGEX = Pattern.compile("\\A" +
            "(?:([a-zA-Z][^:]*):)?" + // scheme
            "([/\\\\\\r\\n\\t]*)" + // slashes
            "([^/\\\\?#]*)" + // authority
            "([/\\\\][^?#]*)?" + // path
            "(?:[?]([^#]*))?" + // query
            "(?:[#](.*))?" + // fragment
            "\\Z", DOTALL);
    private static final int SCHEME = 1, SLASHES = 2, AUTHORITY = 3, PATH = 4, QUERY = 5, FRAGMENT = 6;
    private final static Pattern AUTHORITY_REGEX = Pattern.compile("([^@]*@)?(.*?)(?::([0-9]+))?", DOTALL);
    private final static Pattern IPV4_REGEX = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");

    // According to https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html#uri-syntax-and-components-heading
    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT = "0123456789";
    private static final String ALPHANUM = ALPHA + DIGIT;
    private static final String UNRESERVED = ALPHANUM + "_-!.~'()*";
    private static final String PUNCT = ",;:$&+=";
    private static final String RESERVED = PUNCT + "?/[]@";

    private static final BitSet PATH_ALLOWED = charBitSet("/@" + UNRESERVED + PUNCT);
    private static final BitSet QUERY_ALLOWED = charBitSet(UNRESERVED + RESERVED);

    private static BitSet charBitSet(String chars) {
        BitSet bitSet = new BitSet(128);
        for (char c : chars.toCharArray()) {
            bitSet.set(c);
        }
        return bitSet;
    }

    /**
     * Returns true if the given string begins with a http: or https: URI scheme. Does not enforce the string is a
     * valid URI.
     */
    public static boolean hasHttpOrHttpsScheme(String uri) {
        return startsWithIgnoreCase(uri, "http:") || startsWithIgnoreCase(uri, "https:");
    }

    private static boolean startsWithIgnoreCase(String string, String prefix) {
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Like URI.create() but attempts to percent encode when possible instead of throwing.
     * Note that parseLeniently(s).toString().equals(s) may be false if percent encoding has occurred.
     * @throws IllegalArgumentException if parsing failed
     */
    public static URI parseLeniently(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            Matcher urlMatcher = URL_REGEX.matcher(uri);
            if (!urlMatcher.matches()) {
                throw new IllegalArgumentException("invalid URI: " + uri);
            }

            StringBuilder builder = new StringBuilder();

            String scheme = urlMatcher.group(SCHEME);
            if (scheme != null) {
                builder.append(scheme);
                builder.append(':');
            }

            String slashes = urlMatcher.group(SLASHES);
            if (slashes != null) builder.append(slashes);

            String authority = urlMatcher.group(AUTHORITY);
            if (authority != null) {
                builder.append(authority);
            }

            String path = urlMatcher.group(PATH);
            if (path != null) {
                builder.append(percentEncodeIfNeeded(path, PATH_ALLOWED));
            }

            String query = urlMatcher.group(QUERY);
            if (query != null) {
                builder.append('?');
                builder.append(percentEncodeIfNeeded(query, QUERY_ALLOWED));
            }

            String fragment = urlMatcher.group(FRAGMENT);
            if (fragment != null) {
                builder.append('#');
                builder.append(percentEncodeIfNeeded(fragment, QUERY_ALLOWED));
            }

            return URI.create(builder.toString());
        }
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
               || (c >= 'a' && c <= 'f')
               || (c >= 'A' && c <= 'F');
    }

    private static boolean isASCII(char c) {
        return c <= 127;
    }

    /**
     * Percent encodes a string per the given set of allowed characters. Valid existing percent escapes are
     * preserved instead of double escaped. Unicode characters which are not ASCII, control or space characters
     * are not encoded.
     */
    private static String percentEncodeIfNeeded(String s, BitSet allowed) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (allowed.get(c)) {
                out.append(c);
            } else if (c == '%' && i < s.length() - 2 && isHexDigit(s.charAt(i + 1)) && isHexDigit(s.charAt(i + 2))) {
                out.append(c); // valid existing escape
            } else if (!isASCII(c) && !Character.isISOControl(c) && !Character.isSpaceChar(c)) {
                out.append(c); // an 'other' unicode character
            } else {
                for (byte b : Character.toString(c).getBytes(UTF_8)) {
                    out.append('%').append(String.format("%02x", (int) b));
                }
            }
        }
        return out.toString();
    }

    public static String toNormalizedSurt(String uri) {
        Matcher urlMatcher = URL_REGEX.matcher(uri);
        if (!urlMatcher.matches()) {
            throw new IllegalArgumentException("invalid URL: " + uri);
        }
        String authority = urlMatcher.group(AUTHORITY);
        String path = urlMatcher.group(PATH);
        String query = urlMatcher.group(QUERY);
        String fragment = urlMatcher.group(FRAGMENT);

        Matcher authorityMatcher = AUTHORITY_REGEX.matcher(authority);
        if (!authorityMatcher.matches()) throw new IllegalStateException("authority didn't match");
        String host = authorityMatcher.group(2);
        String port = authorityMatcher.group(3);

        StringBuilder output = new StringBuilder();
        if (IPV4_REGEX.matcher(host).matches()) {
            output.append(host);
        } else {
            List<String> hostSegments = Arrays.asList(host.toLowerCase(Locale.ROOT).split("\\."));
            if (hostSegments.get(0).equals("www")) {
                hostSegments = hostSegments.subList(1, hostSegments.size());
            }
            Collections.reverse(hostSegments);
            output.append(normalizePercentEncoding(String.join(",", hostSegments)));
        }
        if (port != null) {
            output.append(':');
            output.append(port);
        }
        output.append(')');
        if (path != null) {
            output.append(normalizePercentEncoding(normalizePathSegments(path.toLowerCase(Locale.ROOT))));
        } else {
            output.append('/');
        }
        if (query != null) {
            output.append('?');
            String[] params = normalizePercentEncoding(query).toLowerCase(Locale.ROOT).split("&", -1);
            Arrays.sort(params);
            output.append(String.join("&", params));
        }
        if (fragment != null) {
            output.append('#');
            output.append(normalizePercentEncoding(fragment));
        }
        return output.toString();
    }

    static String normalizePathSegments(String path) {
        ArrayList<String> output = new ArrayList<>();
        for (String segment : path.split("/")) {
            switch (segment) {
                case "":
                case ".":
                    break;
                case "..":
                    if (!output.isEmpty()) {
                        output.remove(output.size() - 1);
                    }
                    break;
                default:
                    output.add(segment);
                    break;
            }
        }
        return "/" + String.join("/", output);
    }

    static String normalizePercentEncoding(String s) {
        return percentEncodeIllegals(fullyPercentDecode(s));
    }

    private static String fullyPercentDecode(String s) {
        String prev;
        do {
            prev = s;
            s = percentDecode(s);
        } while (!s.equals(prev));
        return prev;
    }

    public static String percentEncodeIllegals(String s) {
        StringBuilder out = new StringBuilder();
        byte[] bytes = s.getBytes(UTF_8);
        for (byte rawByte : bytes) {
            int b = rawByte & 0xff;
            if (b == '%' || b == '#' || b <= 0x20 || b >= 0x7f) {
                out.append('%').append(String.format("%02x", b));
            } else {
                out.append((char) b);
            }
        }
        return out.toString();
    }

    public static String percentPlusDecode(String s) {
        return percentDecode(s.replace('+', ' '));
    }

    private static String percentDecode(String s) {
        ByteBuffer bb = null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '%') {
                if (bb == null) {
                    bb = ByteBuffer.allocate((s.length() - i) / 3);
                }
                while (i + 2 < s.length() && s.charAt(i) == '%') {
                    int d1 = Character.digit(s.charAt(i + 1), 16);
                    int d2 = Character.digit(s.charAt(i + 2), 16);
                    if (d1 < 0 || d2 < 0) break;
                    bb.put((byte) (d1 << 4 | d2));
                    i += 3;
                }
                bb.flip();
                tryDecodeUtf8(bb, out);
                bb.clear();
                if (i < s.length()) {
                    out.append(s.charAt(i));
                }
            } else {
                out.append(s.charAt(i));
            }
        }
        return out.toString();
    }

    private static void tryDecodeUtf8(ByteBuffer bb, StringBuilder out) {
        CharsetDecoder decoder = UTF_8.newDecoder();
        CharBuffer cb = CharBuffer.allocate(bb.remaining());
        while (bb.hasRemaining()) {
            CoderResult result = decoder.decode(bb, cb, true);
            if (result.isMalformed()) {
                for (int i = 0; i < result.length(); i++) {
                    out.append('%').append(String.format("%02x", bb.get()));
                }
            }
            out.append(cb.flip());
            cb.clear();
        }
    }
}
