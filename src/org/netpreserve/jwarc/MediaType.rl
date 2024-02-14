/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018-2023 National Library of Australia and the jwarc contributors
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

    action mark_invalid {
        valid = false;
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
    strict := (type "/" subtype ( OWS ";" OWS parameter )*) $!parse_error;

    parameter_invalid = (any - ";")* %mark_invalid;
    parameter_lenient = parameter | parameter_invalid;
    type_invalid = (any - "/") %{ typeEnd = p; } %mark_invalid;
    type_lenient = type | type_invalid;
    subtype_invalid = (any - ";")* %{ subtypeEnd = p; } %mark_invalid;
    subtype_lenient = subtype | subtype_invalid;
    string_without_slash = (any - "/")* %mark_invalid;
    lenient := ((type_lenient "/" subtype_lenient ( OWS ";" OWS parameter_lenient )*) | string_without_slash) $!parse_error;

    getkey string.charAt(p);

}%%

public class MediaType extends MessageParser {
    private static BitSet tokenChars = new BitSet();
    static {
        "!#$%&'*+-.^_`|~ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".chars().forEach(tokenChars::set);
    }
    %% write data nofinal noerror;
    public static final MediaType GEMINI = MediaType.parse("application/gemini");
    public static final MediaType GEMTEXT = MediaType.parse("text/gemini");
    public static final MediaType JSON = MediaType.parse("application/json");
    public static MediaType HTML = MediaType.parse("text/html");
    public static MediaType HTML_UTF8 = MediaType.parse("text/html;charset=utf-8");
    public static MediaType HTTP = MediaType.parse("application/http");
    public static MediaType HTTP_REQUEST = MediaType.parse("application/http;msgtype=request");
    public static MediaType HTTP_RESPONSE = MediaType.parse("application/http;msgtype=response");
    public static MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
    public static MediaType PLAIN_TEXT = MediaType.parse("text/plain");
    public static MediaType WARC_FIELDS = MediaType.parse("application/warc-fields");
    public static final MediaType WWW_FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded");

    private final String raw;
    private final String type;
    private final String subtype;
    private final Map<String,String> parameters;
    private int hashCode;
    private boolean valid;

    /**
     * Parses a media type string strictly.
     * @throws IllegalArgumentException if the string is not a valid media type
     */
    public static MediaType parse(String string) throws IllegalArgumentException {
        return parse(string, false);
    }

    /**
     * Parses a media type string leniently.
     * <p>
     * This method is more permissive than {@link #parse(String)} and will not throw an exception if the string is
     * invalid. Instead, the returned media type will have {@link #isValid()} return false. Invalid parameters will be
     * ignored and will omitted from {@link #toString()}. The method {@link #raw()} can be used to return the original
     * string.
     */
    public static MediaType parseLeniently(String string) {
        return parse(string, true);
    }

    private static MediaType parse(String string, boolean lenient) {
        Map<String,String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int cs;
        int p = 0;
        int pe = string.length();
        int eof = pe;
        int typeEnd = string.length();
        int subtypeEnd = string.length();
        int nameStart = 0;
        int nameEnd = 0;
        int valueStart = 0;
        StringBuilder buf = new StringBuilder();
        boolean valid = true;

        %%write init;

        cs = lenient ? media_type_en_lenient : media_type_en_strict;

        %%write exec;

        String type;
        String subtype;
        if (valid) {
            type = string.substring(0, typeEnd);
            subtype = string.substring(typeEnd + 1, subtypeEnd);
        } else {
            type = string.substring(0, typeEnd);
            subtype = typeEnd + 1 >= string.length() ? "" : string.substring(typeEnd + 1, subtypeEnd);
        }
        Map<String,String> parameters = Collections.unmodifiableMap(map);
        return new MediaType(string, type, subtype, parameters, valid);
    }

    private MediaType(String raw, String type, String subtype, Map<String,String> parameters, boolean valid) {
        this.raw = raw;
        this.type = type;
        this.subtype = subtype;
        this.parameters = parameters;
        this.valid = valid;
    }

    /**
     * The original unparsed media type string.
     */
    public String raw() {
        return raw;
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

    public boolean isValid() {
        return valid;
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
        sb.append(type);
        if (!subtype.isEmpty()) {
            sb.append('/').append(subtype);
        }
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
        return new MediaType(null, type, subtype, Collections.emptyMap(), valid);
    }

    private static boolean validToken(String s) {
        return s.chars().allMatch(tokenChars::get);
    }
}
