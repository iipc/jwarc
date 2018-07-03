
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

// line 1 "WarcHeaderParser.rl"
// recompile: ragel -J WarcHeaderParser.rl -o WarcHeaderParser.java
// diagram:   ragel -Vp WarcHeaderParser.rl | dot -TPng | feh -

// line 45 "WarcHeaderParser.rl"


package org.netpreserve.jwarc.lowlevel;

import java.nio.ByteBuffer;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class WarcHeaderParser {
    private final WarcHeaderHandler handler;
    private int cs;
    private byte[] buf = new byte[256];
    private int bufPos = 0;
    private int endOfText;
    private int major;
    private int minor;

    public WarcHeaderParser(WarcHeaderHandler handler) {
        this.handler = handler;
        reset();
    }

    public void reset() {
        
// line 33 "WarcHeaderParser.java"
	{
	cs = warc_start;
	}

// line 70 "WarcHeaderParser.rl"
        bufPos = 0;
        if (buf.length > 8192) {
            buf = new byte[256]; // if our buffer grew really big release it
        }
        major = 0;
        minor = 0;
        endOfText = 0;
    }

    public boolean isFinished() {
        return cs == warc_first_final;
    }

    public boolean isError() {
        return cs == warc_error;
    }

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) {
        int p = data.position();
        int pe = data.limit();

        
// line 62 "WarcHeaderParser.java"
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
	_keys = _warc_key_offsets[cs];
	_trans = _warc_index_offsets[cs];
	_klen = _warc_single_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + _klen - 1;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + ((_upper-_lower) >> 1);
			if ( ( data.get(p)) < _warc_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( data.get(p)) > _warc_trans_keys[_mid] )
				_lower = _mid + 1;
			else {
				_trans += (_mid - _keys);
				break _match;
			}
		}
		_keys += _klen;
		_trans += _klen;
	}

	_klen = _warc_range_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + (_klen<<1) - 2;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + (((_upper-_lower) >> 1) & ~1);
			if ( ( data.get(p)) < _warc_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( data.get(p)) > _warc_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

	_trans = _warc_indicies[_trans];
	cs = _warc_trans_targs[_trans];

	if ( _warc_trans_actions[_trans] != 0 ) {
		_acts = _warc_trans_actions[_trans];
		_nacts = (int) _warc_actions[_acts++];
		while ( _nacts-- > 0 )
	{
			switch ( _warc_actions[_acts++] )
			{
	case 0:
// line 9 "WarcHeaderParser.rl"
	{ push(data.get(p)); }
	break;
	case 1:
// line 10 "WarcHeaderParser.rl"
	{ push((byte)' '); }
	break;
	case 2:
// line 11 "WarcHeaderParser.rl"
	{ major = major * 10 + data.get(p) - '0'; }
	break;
	case 3:
// line 12 "WarcHeaderParser.rl"
	{ minor = minor * 10 + data.get(p) - '0'; }
	break;
	case 4:
// line 13 "WarcHeaderParser.rl"
	{ endOfText = bufPos; }
	break;
	case 5:
// line 14 "WarcHeaderParser.rl"
	{ handler.version(new ProtocolVersion("WARC", major, minor)); }
	break;
	case 6:
// line 15 "WarcHeaderParser.rl"
	{ handler.name(new String(buf, 0, bufPos, US_ASCII)); bufPos = 0; }
	break;
	case 7:
// line 16 "WarcHeaderParser.rl"
	{ handler.value(new String(buf, 0, endOfText, UTF_8)); bufPos = 0; endOfText = 0; }
	break;
	case 8:
// line 43 "WarcHeaderParser.rl"
	{ { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
// line 178 "WarcHeaderParser.java"
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
case 5:
	}
	break; }
	}

// line 93 "WarcHeaderParser.rl"

        data.position(p);
    }

    private void push(byte b) {
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    
// line 211 "WarcHeaderParser.java"
private static byte[] init__warc_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    2,    1,    0,    2,    4,
	    0,    2,    7,    0
	};
}

private static final byte _warc_actions[] = init__warc_actions_0();


private static byte[] init__warc_key_offsets_0()
{
	return new byte [] {
	    0,    0,    1,    2,    3,    4,    5,    7,   10,   12,   15,   16,
	   32,   33,   49,   55,   56,   74,   80,   86,   92
	};
}

private static final byte _warc_key_offsets[] = init__warc_key_offsets_0();


private static char[] init__warc_trans_keys_0()
{
	return new char [] {
	   87,   65,   82,   67,   47,   48,   57,   46,   48,   57,   48,   57,
	   13,   48,   57,   10,   13,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   10,   33,   58,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   13,   32,  127,    0,   31,   10,    9,   13,   32,   33,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,    9,   13,   32,  127,    0,   31,    9,   13,   32,  127,
	    0,   31,    9,   13,   32,  127,    0,   31,    0
	};
}

private static final char _warc_trans_keys[] = init__warc_trans_keys_0();


private static byte[] init__warc_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    4,
	    1,    4,    4,    1,    6,    4,    4,    4,    0
	};
}

private static final byte _warc_single_lengths[] = init__warc_single_lengths_0();


private static byte[] init__warc_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    1,    1,    1,    0,    6,
	    0,    6,    1,    0,    6,    1,    1,    1,    0
	};
}

private static final byte _warc_range_lengths[] = init__warc_range_lengths_0();


private static byte[] init__warc_index_offsets_0()
{
	return new byte [] {
	    0,    0,    2,    4,    6,    8,   10,   12,   15,   17,   20,   22,
	   33,   35,   46,   52,   54,   67,   73,   79,   85
	};
}

private static final byte _warc_index_offsets[] = init__warc_index_offsets_0();


private static byte[] init__warc_indicies_0()
{
	return new byte [] {
	    0,    1,    2,    1,    3,    1,    4,    1,    5,    1,    6,    1,
	    7,    6,    1,    8,    1,    9,    8,    1,   10,    1,   11,   12,
	   12,   12,   12,   12,   12,   12,   12,   12,    1,   13,    1,   12,
	   14,   12,   12,   12,   12,   12,   12,   12,   12,    1,   15,   16,
	   15,    1,    1,   17,   18,    1,   19,   20,   19,   21,   21,   21,
	   21,   21,   21,   21,   21,   21,    1,   19,   22,   19,    1,    1,
	   23,   24,   25,   24,    1,    1,   17,   26,   16,   26,    1,    1,
	   17,    1,    0
	};
}

private static final byte _warc_indicies[] = init__warc_indicies_0();


private static byte[] init__warc_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    4,    5,    6,    7,    8,    9,   10,   11,   12,
	   13,   20,   14,   14,   15,   18,   16,   17,   12,   13,   15,   18,
	   19,   15,   19
	};
}

private static final byte _warc_trans_targs[] = init__warc_trans_targs_0();


private static byte[] init__warc_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    5,    0,    7,   11,    0,    0,
	    1,   17,   13,    0,    0,    1,    0,    0,   15,   25,    3,   19,
	   22,    9,    1
	};
}

private static final byte _warc_trans_actions[] = init__warc_trans_actions_0();


static final int warc_start = 1;
static final int warc_first_final = 20;
static final int warc_error = 0;

static final int warc_en_warc_header = 1;


// line 105 "WarcHeaderParser.rl"
}