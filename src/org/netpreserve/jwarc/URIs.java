package org.netpreserve.jwarc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

class URIs {
    private final static Pattern URL_REGEX = Pattern.compile("\\A" +
            "(?:([a-zA-Z][^:]*):)?" + // scheme
            "[/\\\\\\r\\n\\t]*" + // slashes
            "([^/\\\\]*)" + // authority
            "([/\\\\][^?#]*)?" + // path
            "(?:[?]([^#]*))?" + // query
            "(?:[#](.*))?" + // fragment
            "\\Z", DOTALL);

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
}
