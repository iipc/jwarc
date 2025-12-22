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
    private final static Pattern SURT_URL_REGEX = Pattern.compile(
            "\\A(?:(?<scheme>[A-Za-z][A-Za-z0-9+\\-.]*):)?" +
            "(?:(?://(?<authority>[^/?#]*))?" +
            "(?<path>[^?#]*)" +
            "(?:\\?(?<query>[^#]*))?)?" +
            "(?:#(?<fragment>.*))?\\Z", DOTALL);
    private final static Pattern WWW_REGEX = Pattern.compile("www\\d*\\.");
    private final static Pattern HAS_PROTOCOL_REGEX = Pattern.compile("\\A[a-zA-Z][a-zA-Z0-9+\\-.]*:");
    private final static Pattern QUERY_SESSIONID_REGEX = Pattern.compile(
            "(?:jsessionid=[0-9a-zA-Z]{32}"
            + "|phpsessid=[0-9a-zA-Z]{32}"
            + "|sid=[0-9a-zA-Z]{32}"
            + "|aspsessionid[a-zA-Z]{8}=[a-zA-Z]{24}"
            + "|cfid=[^&]+&cftoken=[^&]+"
            + ")(?:&|$)");
    private static final Pattern[] PATH_SESSIONID_REGEXS = new Pattern[]{
            Pattern.compile("/\\([a-z]\\([0-9a-z]{24}\\)\\)(/[^?]+.aspx)"),
            Pattern.compile("/\\([0-9a-z]{24}\\)(/[^?]+.aspx)"),
    };

    // According to https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html#uri-syntax-and-components-heading
    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT = "0123456789";
    private static final String ALPHANUM = ALPHA + DIGIT;
    private static final String UNRESERVED = ALPHANUM + "_-!.~'()*";
    private static final String PUNCT = ",;:$&+=";
    private static final String RESERVED = PUNCT + "?/[]@";

    private static final BitSet PATH_ALLOWED = charBitSet("/@" + UNRESERVED + PUNCT);
    private static final BitSet QUERY_ALLOWED = charBitSet(UNRESERVED + RESERVED);
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

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
                    appendPercentEncoding(out, b);
                }
            }
        }
        return out.toString();
    }

    private static void appendPercentEncoding(StringBuilder out, byte b) {
        out.append('%');
        out.append(HEX_DIGITS[(b >> 4) & 0xf]);
        out.append(HEX_DIGITS[b & 0xf]);
    }

    /**
     * Converts a given URI into its normalized SURT (Sort-friendly URI Reordering Transform) format.
     * <p>
     * There are many slightly different implementations of SURT. This one tries to produce the same output as the
     * Python <a href="https://github.com/internetarchive/surt">surt</a> module for compatibility with pywb.
     */
    public static String toNormalizedSurt(String uri) {
        if (uri.startsWith("filedesc")) return uri;

        uri = trimSpaces(uri);
        uri = uri.replace("\r", "");
        uri = uri.replace("\n", "");
        uri = uri.replace("\t", "");

        if (!uri.isEmpty() && !HAS_PROTOCOL_REGEX.matcher(uri).lookingAt()) {
            uri = "http://" + uri;
        }

        Matcher urlMatcher = SURT_URL_REGEX.matcher(uri);
        if (!urlMatcher.matches()) {
            return uri; // shouldn't be possible
        }
        String scheme = urlMatcher.group("scheme");
        String authority = urlMatcher.group("authority");
        String path = urlMatcher.group("path");
        String query = urlMatcher.group("query");
        String fragment = urlMatcher.group("fragment");

        String host = null;
        String port = null;

        if (authority != null) {
            int atIndex = authority.indexOf('@');
            int colonIndex = -1;
            for (int i = authority.length() - 1; i > atIndex; i--) {
                char c = authority.charAt(i);
                if (c == ':') {
                    colonIndex = i;
                    break;
                } else if (!isAsciiDigit(c)) {
                    break;
                }
            }
            if (colonIndex >= 0) {
                host = authority.substring(atIndex + 1, colonIndex);
                port = authority.substring(colonIndex + 1);
            } else {
                host = authority.substring(atIndex + 1);
            }
        }

        StringBuilder output = new StringBuilder();
        if (host == null) {
            if (scheme != null) {
                output.append(scheme);
                output.append(':');
            }
        } else {
            // remove IPv6 brackets
            if (host.startsWith("[")) {
                host = host.substring(1, host.length() - 1);
            }

            host = host.toLowerCase(Locale.ROOT);
            host = trimWWW(host);
            host = reverseHost(host);
            output.append(normalizePercentEncoding(host));
            if (port != null && !port.isEmpty() && !isDefaultPort(scheme, port)) {
                output.append(':');
                output.append(port);
            }
            output.append(')');
        }

        if (path != null) {
            path = fullyPercentDecode(path);
            path = path.toLowerCase(Locale.ROOT);
            if (host != null) path = normalizePathSegments(path);
            for (Pattern PATH_SESSIONID : PATH_SESSIONID_REGEXS) {
                path = PATH_SESSIONID.matcher(path).replaceFirst("$1");
            }
            path = percentEncodeIllegals(path);
            if (path.length() > 1 && path.endsWith("/")) {
                output.append(path, 0, path.length() - 1);
            } else {
                output.append(path);
            }
        } else {
            output.append('/');
        }
        if (query != null && !query.isEmpty()) {
            output.append('?');
            query = normalizePercentEncoding(query);
            query = query.toLowerCase(Locale.ROOT);
            query = QUERY_SESSIONID_REGEX.matcher(query).replaceAll("");
            String[] params = query.split("&", -1);
            Arrays.sort(params);
            boolean first = true;
            for (String param : params) {
                if (!first) output.append('&');
                first = false;
                output.append(param);
            }
        }
        if (fragment != null) {
            output.append('#');
            output.append(normalizePercentEncoding(fragment));
        }
        return output.toString();
    }

    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static String trimWWW(String host) {
        Matcher matcher = WWW_REGEX.matcher(host);
        if (matcher.lookingAt()) {
            return host.substring(matcher.end());
        }
        return host;
    }

    private static String reverseHost(String s) {
        if (s == null || s.isEmpty()) return s;

        StringBuilder result = new StringBuilder();
        int end = s.length();

        while (end > 0) {
            int start = s.lastIndexOf('.', end - 1);

            if (result.length() > 0) {
                result.append(',');
            }

            if (start == -1) {
                if (end == s.length()) return s;
                result.append(s, 0, end);
                break;
            } else {
                result.append(s, start + 1, end);
                end = start;
            }
        }

        return result.toString();
    }

    /**
     * Removes leading and trailing spaces from a string.
     */
    private static String trimSpaces(String s) {
        int start = 0;
        int end = s.length();

        while (start < end && s.charAt(start) == ' ') {
            start++;
        }

        while (end > start && s.charAt(end - 1) == ' ') {
            end--;
        }

        return s.substring(start, end);
    }

    private static boolean isDefaultPort(String scheme, String port) {
        return (scheme.equalsIgnoreCase("http") && port.equals("80")) ||
               (scheme.equalsIgnoreCase("https") && port.equals("443"));
    }

    static String normalizePathSegments(String path) {
        if (path == null || path.isEmpty()) return "/";

        int len = path.length();
        int[] segmentStarts = new int[len / 2 + 1];
        int[] segmentEnds = new int[len / 2 + 1];
        int size = 0;

        for (int i = 0; i < len; ) {
            // Skip slashes
            if (path.charAt(i) == '/') {
                i++;
                continue;
            }

            // Find end of segment
            int start = i;
            while (i < len && path.charAt(i) != '/') {
                i++;
            }
            int end = i;

            int segmentLen = end - start;
            //noinspection StatementWithEmptyBody
            if (segmentLen == 1 && path.charAt(start) == '.') {
                // Ignore "."
            } else if (segmentLen == 2 && path.charAt(start) == '.' && path.charAt(start + 1) == '.') {
                // Handle ".."
                if (size > 0) size--;
            } else {
                // Valid segment
                segmentStarts[size] = start;
                segmentEnds[size] = end;
                size++;
            }
        }

        if (size == 0) return "/";

        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < size; i++) {
            sb.append('/').append(path, segmentStarts[i], segmentEnds[i]);
        }
        return sb.toString();
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
        // optimisation: in the common case there are none, return the original string
        boolean seen = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' || c == '#' || c <= 0x20 || c >= 0x7f) {
                seen = true;
                break;
            }
        }
        if (!seen) return s;

        StringBuilder out = new StringBuilder();
        byte[] bytes = s.getBytes(UTF_8);
        for (byte rawByte : bytes) {
            int b = rawByte & 0xff;
            if (b == '%' || b == '#' || b <= 0x20 || b >= 0x7f) {
                appendPercentEncoding(out, (byte) b);
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
        if (s.indexOf('%') == -1) return s;
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
                    appendPercentEncoding(out, bb.get());
                }
            }
            out.append(cb.flip());
            cb.clear();
        }
    }
}
