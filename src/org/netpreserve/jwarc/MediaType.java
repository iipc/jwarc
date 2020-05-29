
// line 1 "MediaType.rl"
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

// recompile: ragel -J MediaType.rl -o MediaType.java
// diagram:   ragel -Vp MediaType.rl | dot -Tpng | feh -

package org.netpreserve.jwarc;

import java.util.*;


// line 53 "MediaType.rl"


public class MediaType extends MessageParser {
    private static BitSet tokenChars = new BitSet();
    static {
        "!#$%&'*+-.^_`|~ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".chars().forEach(tokenChars::set);
    }
    
// line 26 "MediaType.java"
private static byte[] init__media_type_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   10,    1,
	   11,    2,    4,    3
	};
}

private static final byte _media_type_actions[] = init__media_type_actions_0();


private static short[] init__media_type_key_offsets_0()
{
	return new short [] {
	    0,    0,   15,   29,   44,   47,   64,   80,   95,  102,  109,  114,
	  132,  150
	};
}

private static final short _media_type_key_offsets[] = init__media_type_key_offsets_0();


private static char[] init__media_type_trans_keys_0()
{
	return new char [] {
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   33,   47,  124,  126,   35,   39,   42,   43,   45,
	   57,   65,   90,   94,  122,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,    9,   32,   59,    9,
	   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   33,   61,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   34,  124,  126,   33,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   34,
	   92,  127,    0,    8,   10,   31,   34,   92,  127,    0,    8,   10,
	   31,  127,    0,    8,   10,   31,    9,   32,   33,   59,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   32,   33,   59,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,    9,   32,   59,    0
	};
}

private static final char _media_type_trans_keys[] = init__media_type_trans_keys_0();


private static byte[] init__media_type_single_lengths_0()
{
	return new byte [] {
	    0,    3,    4,    3,    3,    5,    4,    3,    3,    3,    1,    6,
	    6,    3
	};
}

private static final byte _media_type_single_lengths[] = init__media_type_single_lengths_0();


private static byte[] init__media_type_range_lengths_0()
{
	return new byte [] {
	    0,    6,    5,    6,    0,    6,    6,    6,    2,    2,    2,    6,
	    6,    0
	};
}

private static final byte _media_type_range_lengths[] = init__media_type_range_lengths_0();


private static byte[] init__media_type_index_offsets_0()
{
	return new byte [] {
	    0,    0,   10,   20,   30,   34,   46,   57,   67,   73,   79,   83,
	   96,  109
	};
}

private static final byte _media_type_index_offsets[] = init__media_type_index_offsets_0();


private static byte[] init__media_type_indicies_0()
{
	return new byte [] {
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    0,    1,    2,
	    1,    1,    1,    1,    1,    1,    1,    0,    3,    3,    3,    3,
	    3,    3,    3,    3,    3,    0,    4,    4,    5,    0,    5,    5,
	    6,    6,    6,    6,    6,    6,    6,    6,    6,    0,    7,    8,
	    7,    7,    7,    7,    7,    7,    7,    7,    0,   10,    9,    9,
	    9,    9,    9,    9,    9,    9,    0,   12,   13,    0,    0,    0,
	   11,   15,   16,    0,    0,    0,   14,    0,    0,    0,   17,   18,
	   18,    3,   19,    3,    3,    3,    3,    3,    3,    3,    3,    0,
	   20,   20,   21,   22,   21,   21,   21,   21,   21,   21,   21,   21,
	    0,   23,   23,   24,    0,    0
	};
}

private static final byte _media_type_indicies[] = init__media_type_indicies_0();


private static byte[] init__media_type_trans_targs_0()
{
	return new byte [] {
	    0,    2,    3,   11,    4,    5,    6,    6,    7,   12,    8,    9,
	   13,   10,    9,   13,   10,    8,    4,    5,    4,   12,    5,    4,
	    5
	};
}

private static final byte _media_type_trans_targs[] = init__media_type_trans_targs_0();


private static byte[] init__media_type_trans_actions_0()
{
	return new byte [] {
	    5,    0,   21,    0,    0,    0,   15,    0,   17,   19,   13,    7,
	    0,    0,   25,    9,    9,   11,   23,   23,    1,    0,    1,    3,
	    3
	};
}

private static final byte _media_type_trans_actions[] = init__media_type_trans_actions_0();


private static byte[] init__media_type_eof_actions_0()
{
	return new byte [] {
	    0,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,   23,
	    1,    3
	};
}

private static final byte _media_type_eof_actions[] = init__media_type_eof_actions_0();


static final int media_type_start = 1;


// line 61 "MediaType.rl"
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

        
// line 193 "MediaType.java"
	{
	cs = media_type_start;
	}

// line 91 "MediaType.rl"
        
// line 200 "MediaType.java"
	{
	int _klen;
	int _trans = 0;
	int _acts;
	int _nacts;
	int _keys;
	int _goto_targ = 0;

	_goto: while (true) {
	switch ( _goto_targ ) {
	case 0:
	if ( p == pe ) {
		_goto_targ = 4;
		continue _goto;
	}
	if ( cs == 0 ) {
		_goto_targ = 5;
		continue _goto;
	}
case 1:
	_match: do {
	_keys = _media_type_key_offsets[cs];
	_trans = _media_type_index_offsets[cs];
	_klen = _media_type_single_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + _klen - 1;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + ((_upper-_lower) >> 1);
			if ( ( string.charAt(p)) < _media_type_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( string.charAt(p)) > _media_type_trans_keys[_mid] )
				_lower = _mid + 1;
			else {
				_trans += (_mid - _keys);
				break _match;
			}
		}
		_keys += _klen;
		_trans += _klen;
	}

	_klen = _media_type_range_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + (_klen<<1) - 2;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + (((_upper-_lower) >> 1) & ~1);
			if ( ( string.charAt(p)) < _media_type_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( string.charAt(p)) > _media_type_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

	_trans = _media_type_indicies[_trans];
	cs = _media_type_trans_targs[_trans];

	if ( _media_type_trans_actions[_trans] != 0 ) {
		_acts = _media_type_trans_actions[_trans];
		_nacts = (int) _media_type_actions[_acts++];
		while ( _nacts-- > 0 )
	{
			switch ( _media_type_actions[_acts++] )
			{
	case 0:
// line 17 "MediaType.rl"
	{
        String name = string.substring(nameStart, nameEnd);
        String value = string.substring(valueStart, p);
        map.putIfAbsent(name, value);
    }
	break;
	case 1:
// line 23 "MediaType.rl"
	{
        String name = string.substring(nameStart, nameEnd);
        String value = buf.toString();
        map.putIfAbsent(name, value);
    }
	break;
	case 2:
// line 29 "MediaType.rl"
	{
        if (p >= 0) { /* this if statement is just to stop javac complaining about unreachable code */
            throw new IllegalArgumentException("parse error at position " + p + ": " + getErrorContext(string, p, 40));
        }
    }
	break;
	case 3:
// line 40 "MediaType.rl"
	{valueStart = p; }
	break;
	case 4:
// line 41 "MediaType.rl"
	{ buf.append(string, valueStart, p); }
	break;
	case 5:
// line 42 "MediaType.rl"
	{ buf.append(string.charAt(p)); }
	break;
	case 6:
// line 43 "MediaType.rl"
	{ buf.setLength(0); }
	break;
	case 7:
// line 44 "MediaType.rl"
	{ nameStart = p; }
	break;
	case 8:
// line 44 "MediaType.rl"
	{ nameEnd = p; }
	break;
	case 9:
// line 45 "MediaType.rl"
	{ valueStart = p; }
	break;
	case 10:
// line 47 "MediaType.rl"
	{ typeEnd = p; }
	break;
	case 11:
// line 48 "MediaType.rl"
	{ subtypeEnd = p; }
	break;
// line 340 "MediaType.java"
			}
		}
	}

case 2:
	if ( cs == 0 ) {
		_goto_targ = 5;
		continue _goto;
	}
	if ( ++p != pe ) {
		_goto_targ = 1;
		continue _goto;
	}
case 4:
	if ( p == eof )
	{
	int __acts = _media_type_eof_actions[cs];
	int __nacts = (int) _media_type_actions[__acts++];
	while ( __nacts-- > 0 ) {
		switch ( _media_type_actions[__acts++] ) {
	case 0:
// line 17 "MediaType.rl"
	{
        String name = string.substring(nameStart, nameEnd);
        String value = string.substring(valueStart, p);
        map.putIfAbsent(name, value);
    }
	break;
	case 1:
// line 23 "MediaType.rl"
	{
        String name = string.substring(nameStart, nameEnd);
        String value = buf.toString();
        map.putIfAbsent(name, value);
    }
	break;
	case 2:
// line 29 "MediaType.rl"
	{
        if (p >= 0) { /* this if statement is just to stop javac complaining about unreachable code */
            throw new IllegalArgumentException("parse error at position " + p + ": " + getErrorContext(string, p, 40));
        }
    }
	break;
	case 11:
// line 48 "MediaType.rl"
	{ subtypeEnd = p; }
	break;
// line 389 "MediaType.java"
		}
	}
	}

case 5:
	}
	break; }
	}

// line 92 "MediaType.rl"

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
