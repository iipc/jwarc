package jwarc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WarcDigest {
    final String algorithm;
    final String digestValue;

    public WarcDigest(String algorithm, String digestValue) {
        this.algorithm = algorithm;
        this.digestValue = digestValue;
    }

    public String toString() {
        return algorithm + ":" + digestValue;
    }

    private static final String SEPARATORS = "\\^|<>@,;:\\\\\"/\\[\\]?={} \\t";
    private static final String TOKEN = "[\\p{ASCII}&&[^\\p{Cntrl}" + SEPARATORS + "]]+";
    private static final Pattern PATTERN = Pattern.compile("^(" + TOKEN + "):(" + TOKEN + ")$");

    public static WarcDigest parse(String s) {
        Matcher m = PATTERN.matcher(s);
        if (m.matches()) {
            return new WarcDigest(m.group(1), m.group(2));
        } else {
            throw new IllegalArgumentException("invalid labelled digest: " + s);
        }
    }
}
