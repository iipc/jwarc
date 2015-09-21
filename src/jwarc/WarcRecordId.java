package jwarc;

import java.net.URI;

public class WarcRecordId {
    private final URI uri;

    WarcRecordId(URI uri) {
        this.uri = uri;
    }

    public URI toURI() {
        return uri;
    }

    public String toString() {
        return "<" + uri.toString() + ">";
    }

    public static WarcRecordId parse(String bracketedUri) {
        /*
         * The BNF grammar for WARC-Target-URI and WARC-Profile in the WARC 1.0 specification is inconsistent with its
         * own examples.  The grammar specifies that URIs must be wrapped in "<" and ">", which examples follow except
         * for those two fields.  Most implementations follow the examples.  Let's accept either form when parsing.
         */
        if (bracketedUri == null) {
            return null;
        } else if (bracketedUri.startsWith("<") && bracketedUri.endsWith(">")) {
            return new WarcRecordId(URI.create(bracketedUri.substring(1, bracketedUri.length() - 1)));
        } else {
            return new WarcRecordId(URI.create(bracketedUri));
        }
    }
}
