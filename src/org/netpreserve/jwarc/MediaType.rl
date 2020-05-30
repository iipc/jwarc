/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

// recompile: ragel -J MediaType.rl -o MediaType.java
// diagram:   ragel -Vp MediaType.rl | dot -Tpng | feh -

package org.netpreserve.jwarc;

import java.util.*;

%%{

    machine media_type;

    action end_value_token {
        String name = string.substring(nameStart, nameEnd);
        String value = string.substring(valueStart, p);
        map.putIfAbsent(name, value);
    }

    action end_value_quoted {
        String name = string.substring(nameStart, nameEnd);
        String value = buf.toString();
        map.putIfAbsent(name, value);
    }

    action parse_error {
        if (p >= 0) { /* this if statement is just to stop javac complaining about unreachable code */
            throw new IllegalArgumentException("parse error at position " + p + ": " + getErrorContext(string, p, 40));
        }
    }

    OWS = (" " | "\t")*;
    tchar = "!" | "#" | "$" | "%" | "&" | "'" | "*" | "+" | "-" | "." |
            "^" | "_" | "`" | "|" | "~" | digit | alpha;
    token = tchar+;
    obs_text = any - ascii;
    qdtext = ("\t" | " " | 0x21 | 0x23..0x5b | 0x5d..0x7e | obs_text)+ >{valueStart = p; }
             %{ buf.append(string, valueStart, p); };
    quoted_pair = "\\" ( "\t" | " " | graph | obs_text ) ${ buf.append(string.charAt(p)); };
    quoted_string = ('"' (qdtext | quoted_pair)* '"') >{ buf.setLength(0); } %end_value_quoted;
    name = token >{ nameStart = p; } %{ nameEnd = p; };
    value_token = token >{ valueStart = p; } %end_value_token;
    parameter = name "=" (value_token | quoted_string );
    type = token %{ typeEnd = p; };
    subtype = token %{ subtypeEnd = p; };
    media_type := (type "/" subtype ( OWS ";" OWS parameter )*) $!parse_error;

    getkey string.charAt(p);

}%%

public class MediaType extends MessageParser {
    private static BitSet tokenChars = new BitSet();
    static {
        "!#$%&'*+-.^_`|~ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".chars().forEach(tokenChars::set);
    }
    %% write data nofinal noerror noentry;
    public static MediaType HTML = MediaType.parse("text/html");
    public static MediaType HTML_UTF8 = MediaType.parse("text/html;charset=utf-8s");
    public static MediaType HTTP = MediaType.parse("application/http");
    public static MediaType HTTP_REQUEST = MediaType.parse("application/http;msgtype=request");
    public static MediaType HTTP_RESPONSE = MediaType.parse("application/http;msgtype=response");
    public static MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
    public static MediaType WARC_FIELDS = MediaType.parse("application/warc-fields");

    private final String type;
    private final String subtype;
    private final Map<String,String> parameters;
    private int hashCode;

    /**
     * Parses a media type string.
     */
    public static MediaType parse(String string) {
        Map<String,String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int p = 0;
        int pe = string.length();
        int eof = pe;
        int cs;
        int typeEnd = 0;
        int subtypeEnd = 0;
        int nameStart = 0;
        int nameEnd = 0;
        int valueStart = 0;
        StringBuilder buf = new StringBuilder();

        %%write init;
        %%write exec;

        String type = string.substring(0, typeEnd);
        String subtype = string.substring(typeEnd + 1, subtypeEnd);
        Map<String,String> parameters = Collections.unmodifiableMap(map);
        return new MediaType(type, subtype, parameters);
    }

    private MediaType(String type, String subtype, Map<String,String> parameters) {
        this.type = type;
        this.subtype = subtype;
        this.parameters = parameters;
    }

    public String type() {
        return type;
    }

    public String subtype() {
        return subtype;
    }

    public Map<String,String> parameters() {
        return parameters;
    }

    /**
     * Two media types are considered equal if their type, subtype and parameter names are equal case-insensitively
     * and if the parameter values are equal case-sensitively.
     */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MediaType that = (MediaType) o;
		if (hashCode != 0 && hashCode != that.hashCode && that.hashCode != 0) return false;
		return subtype.equalsIgnoreCase(that.subtype) &&
				type.equalsIgnoreCase(that.type) &&
				parameters.equals(that.parameters);
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			int result = type.toLowerCase(Locale.ROOT).hashCode();
			result = 31 * result + subtype.toLowerCase(Locale.ROOT).hashCode();
			for (Map.Entry<String, String> entry : parameters.entrySet()) {
				result = 31 * result + entry.getKey().toLowerCase(Locale.ROOT).hashCode();
				result = 31 * result + entry.getValue().hashCode();
			}
			hashCode = result;
		}
		return hashCode;
	}

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append('/').append(subtype);
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            sb.append(';');
            sb.append(parameter.getKey());
            sb.append('=');
            String value = parameter.getValue();
            if (!validToken(value)) {
                sb.append('"');
                sb.append(value.replace("\\", "\\\\").replace("\"", "\\\""));
                sb.append('"');
            } else {
                sb.append(value);
            }
        }
        return sb.toString();
    }

    /**
     * The base type and subtype without any parameters.
     */
    public MediaType base() {
        return new MediaType(type, subtype, Collections.emptyMap());
    }

    private static boolean validToken(String s) {
        return s.chars().allMatch(tokenChars::get);
    }
}
