
// line 1 "MediaType.rl"
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018-2023 National Library of Australia and the jwarc contributors
 */

// recompile: ragel -J MediaType.rl -o MediaType.java
// diagram:   ragel -Vp MediaType.rl | dot -Tpng | feh -

package org.netpreserve.jwarc;

import java.util.*;


// line 66 "MediaType.rl"


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
	   11,    1,   12,    2,    0,    3,    2,    1,    3,    2,    4,    3,
	    2,    5,    4,    2,    6,    3,    2,    7,    5,    2,    8,    4,
	    2,   13,    3,    2,   14,    3,    3,    4,    1,    3,    3,    5,
	    4,    3,    3,    8,    5,    4,    3,    9,    5,    4,    3,   10,
	    5,    4,    3,   11,   13,    3,    3,   12,   14,    3,    4,    0,
	    5,    4,    3
	};
}

private static final byte _media_type_actions[] = init__media_type_actions_0();


private static short[] init__media_type_key_offsets_0()
{
	return new short [] {
	    0,    0,   15,   29,   44,   47,   64,   80,   95,  102,  109,  114,
	  128,  129,  130,  148,  151,  169,  172,  191,  209,  227,  235,  243,
	  246,  268,  291,  313,  335,  341,  363,  371,  389,  403,  417,  435,
	  453
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
	   31,  127,    0,    8,   10,   31,   33,   47,  124,  126,   35,   39,
	   42,   43,   45,   57,   65,   90,   94,  122,   47,   47,    9,   32,
	   33,   59,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   32,   59,    9,   32,   33,   59,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   32,   59,    9,   32,   33,   59,   61,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   32,   34,   59,  124,  126,   33,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,    9,   32,   33,   59,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   32,   34,   59,   92,  127,    0,   31,    9,   32,   34,   59,   92,
	  127,    0,   31,    9,   32,   59,    9,   32,   34,   59,   92,  124,
	  126,  127,    0,   31,   33,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   32,   34,   59,   61,   92,  124,  126,
	  127,    0,   31,   33,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   32,   34,   59,   92,  124,  126,  127,    0,
	   31,   33,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   32,   34,   59,   92,  124,  126,  127,    0,   31,   33,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   32,   59,  127,    0,   31,    9,   32,   34,   59,   92,  124,  126,
	  127,    0,   31,   33,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   32,   34,   59,   92,  127,    0,   31,    9,
	   32,   33,   59,  124,  126,   35,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,   33,   47,  124,  126,   35,   39,   42,
	   43,   45,   57,   65,   90,   94,  122,   33,   47,  124,  126,   35,
	   39,   42,   43,   45,   57,   65,   90,   94,  122,    9,   32,   33,
	   59,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   32,   33,   59,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   32,   59,
	    0
	};
}

private static final char _media_type_trans_keys[] = init__media_type_trans_keys_0();


private static byte[] init__media_type_single_lengths_0()
{
	return new byte [] {
	    0,    3,    4,    3,    3,    5,    4,    3,    3,    3,    1,    4,
	    1,    1,    6,    3,    6,    3,    7,    6,    6,    6,    6,    3,
	    8,    9,    8,    8,    4,    8,    6,    6,    4,    4,    6,    6,
	    3
	};
}

private static final byte _media_type_single_lengths[] = init__media_type_single_lengths_0();


private static byte[] init__media_type_range_lengths_0()
{
	return new byte [] {
	    0,    6,    5,    6,    0,    6,    6,    6,    2,    2,    2,    5,
	    0,    0,    6,    0,    6,    0,    6,    6,    6,    1,    1,    0,
	    7,    7,    7,    7,    1,    7,    1,    6,    5,    5,    6,    6,
	    0
	};
}

private static final byte _media_type_range_lengths[] = init__media_type_range_lengths_0();


private static short[] init__media_type_index_offsets_0()
{
	return new short [] {
	    0,    0,   10,   20,   30,   34,   46,   57,   67,   73,   79,   83,
	   93,   95,   97,  110,  114,  127,  131,  145,  158,  171,  179,  187,
	  191,  207,  224,  240,  256,  262,  278,  286,  299,  309,  319,  332,
	  345
	};
}

private static final short _media_type_index_offsets[] = init__media_type_index_offsets_0();


private static byte[] init__media_type_indicies_0()
{
	return new byte [] {
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    0,    1,    2,
	    1,    1,    1,    1,    1,    1,    1,    0,    3,    3,    3,    3,
	    3,    3,    3,    3,    3,    0,    4,    4,    5,    0,    5,    5,
	    6,    6,    6,    6,    6,    6,    6,    6,    6,    0,    7,    8,
	    7,    7,    7,    7,    7,    7,    7,    7,    0,   10,    9,    9,
	    9,    9,    9,    9,    9,    9,    0,   12,   13,    0,    0,    0,
	   11,   15,   16,    0,    0,    0,   14,    0,    0,    0,   17,   19,
	    0,   19,   19,   19,   19,   19,   19,   19,   18,   21,   20,    0,
	   20,   23,   23,   24,   25,   24,   24,   24,   24,   24,   24,   24,
	   24,   22,   23,   23,   25,   22,   27,   27,   28,   27,   28,   28,
	   28,   28,   28,   28,   28,   28,   26,   29,   29,   27,   26,   29,
	   29,   30,   27,   31,   30,   30,   30,   30,   30,   30,   30,   30,
	   26,   29,   29,   33,   27,   32,   32,   32,   32,   32,   32,   32,
	   32,   26,   34,   34,   35,   36,   35,   35,   35,   35,   35,   35,
	   35,   35,   26,   37,   37,   39,   40,   41,   26,   26,   38,   42,
	   42,   44,   45,   46,   26,   26,   43,   47,   47,   48,   26,   45,
	   45,   44,   45,   46,   49,   49,   26,   26,   49,   49,   49,   49,
	   49,   49,   43,   42,   42,   44,   45,   51,   46,   50,   50,   26,
	   26,   50,   50,   50,   50,   50,   50,   43,   42,   42,   53,   45,
	   46,   52,   52,   26,   26,   52,   52,   52,   52,   52,   52,   43,
	   54,   54,   44,   56,   46,   55,   55,   26,   26,   55,   55,   55,
	   55,   55,   55,   43,   57,   57,   59,   26,   26,   58,   40,   40,
	   39,   40,   41,   60,   60,   26,   26,   60,   60,   60,   60,   60,
	   60,   38,   61,   61,   39,   62,   41,   26,   26,   38,   63,   63,
	   24,   64,   24,   24,   24,   24,   24,   24,   24,   24,   22,   65,
	   66,   65,   65,   65,   65,   65,   65,   65,   20,   65,   67,   65,
	   65,   65,   65,   65,   65,   65,   20,   68,   68,    3,   69,    3,
	    3,    3,    3,    3,    3,    3,    3,    0,   70,   70,   71,   72,
	   71,   71,   71,   71,   71,   71,   71,   71,    0,   73,   73,   74,
	    0,    0
	};
}

private static final byte _media_type_indicies[] = init__media_type_indicies_0();


private static byte[] init__media_type_trans_targs_0()
{
	return new byte [] {
	    0,    2,    3,   34,    4,    5,    6,    6,    7,   35,    8,    9,
	   36,   10,    9,   36,   10,    8,   12,   32,   13,   14,   15,   15,
	   31,   16,   17,   16,   18,   17,   18,   19,   20,   21,   17,   20,
	   16,   22,   22,   23,   24,   28,   22,   22,   23,   24,   28,   17,
	   16,   25,   25,   26,   27,   30,   22,   27,   24,   21,   21,   29,
	   25,   22,   24,   15,   16,   33,   14,   14,    4,    5,    4,   35,
	    5,    4,    5
	};
}

private static final byte _media_type_trans_targs[] = init__media_type_trans_targs_0();


private static byte[] init__media_type_trans_actions_0()
{
	return new byte [] {
	    5,    0,   23,    0,    0,    0,   17,    0,   19,   21,   15,    9,
	    0,    0,   36,   11,   11,   13,    0,    0,    0,   48,    0,   51,
	    0,   51,    0,    7,   17,    7,    0,   19,   21,   15,   27,    0,
	   27,   33,    9,    0,   33,    0,   58,   36,   11,   58,   11,   30,
	   30,   62,   36,   66,   70,   42,   82,   36,   82,   39,   13,   39,
	   45,   54,   54,   78,   78,    0,   74,   23,   25,   25,    1,    0,
	    1,    3,    3
	};
}

private static final byte _media_type_trans_actions[] = init__media_type_trans_actions_0();


private static byte[] init__media_type_eof_actions_0()
{
	return new byte [] {
	    0,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    7,
	    7,    7,   51,   51,    7,    7,    7,    7,   27,    7,    7,   30,
	    7,    7,    7,   27,    7,    7,   30,   78,    7,    7,   25,    1,
	    3
	};
}

private static final byte _media_type_eof_actions[] = init__media_type_eof_actions_0();


static final int media_type_start = 11;

static final int media_type_en_strict = 1;
static final int media_type_en_lenient = 11;


// line 74 "MediaType.rl"
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

        
// line 290 "MediaType.java"
	{
	cs = media_type_start;
	}

// line 129 "MediaType.rl"

        cs = lenient ? media_type_en_lenient : media_type_en_strict;

        
// line 300 "MediaType.java"
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
// line 35 "MediaType.rl"
	{
        valid = false;
    }
	break;
	case 4:
// line 44 "MediaType.rl"
	{valueStart = p; }
	break;
	case 5:
// line 45 "MediaType.rl"
	{ buf.append(string, valueStart, p); }
	break;
	case 6:
// line 46 "MediaType.rl"
	{ buf.append(string.charAt(p)); }
	break;
	case 7:
// line 47 "MediaType.rl"
	{ buf.setLength(0); }
	break;
	case 8:
// line 48 "MediaType.rl"
	{ nameStart = p; }
	break;
	case 9:
// line 48 "MediaType.rl"
	{ nameEnd = p; }
	break;
	case 10:
// line 49 "MediaType.rl"
	{ valueStart = p; }
	break;
	case 11:
// line 51 "MediaType.rl"
	{ typeEnd = p; }
	break;
	case 12:
// line 52 "MediaType.rl"
	{ subtypeEnd = p; }
	break;
	case 13:
// line 57 "MediaType.rl"
	{ typeEnd = p; }
	break;
	case 14:
// line 59 "MediaType.rl"
	{ subtypeEnd = p; }
	break;
// line 454 "MediaType.java"
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
	case 3:
// line 35 "MediaType.rl"
	{
        valid = false;
    }
	break;
	case 12:
// line 52 "MediaType.rl"
	{ subtypeEnd = p; }
	break;
	case 14:
// line 59 "MediaType.rl"
	{ subtypeEnd = p; }
	break;
// line 513 "MediaType.java"
		}
	}
	}

case 5:
	}
	break; }
	}

// line 133 "MediaType.rl"

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
