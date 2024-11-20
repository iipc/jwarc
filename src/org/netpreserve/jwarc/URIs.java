package org.netpreserve.jwarc;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

public class URIs {
    private final static Pattern URL_REGEX = Pattern.compile("\\A" +
            "(?:([a-zA-Z][^:]*):)?" + // scheme
            "[/\\\\\\r\\n\\t]*" + // slashes
            "([^/\\\\]*)" + // authority
            "([/\\\\][^?#]*)?" + // path
            "(?:[?]([^#]*))?" + // query
            "(?:[#](.*))?" + // fragment
            "\\Z", DOTALL);
    private final static Pattern AUTHORITY_REGEX = Pattern.compile("([^@]*@)?(.*?)(?::([0-9]+))?", DOTALL);
    private final static Pattern IPV4_REGEX = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");

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

    public static URI parseLeniently(String uri) {
        Matcher m = URL_REGEX.matcher(uri);
        if (!m.matches()) {
            throw new IllegalArgumentException();
        }
        try {
            return new URI(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String toNormalizedSurt(String uri) {
        Matcher urlMatcher = URL_REGEX.matcher(uri);
        if (!urlMatcher.matches()) {
            throw new IllegalArgumentException("invalid URL: " + uri);
        }
        String authority = urlMatcher.group(2);
        String path = urlMatcher.group(3);
        String query = urlMatcher.group(4);
        String fragment = urlMatcher.group(5);

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
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
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
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
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
