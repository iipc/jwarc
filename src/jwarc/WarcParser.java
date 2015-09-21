
// line 1 "WarcParser.rl"

// line 33 "WarcParser.rl"


package jwarc;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

class WarcParser {
    final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    Map<String,String> fields;
    int cs;
    String fieldName;

    int versionMinor;
    int versionMajor;

    WarcParser() {
        reset();
    }

    void reset() {
        
// line 30 "WarcParser.java"
	{
	cs = warc_start;
	}

// line 57 "WarcParser.rl"
        buf.reset();
        versionMajor = 0;
        versionMinor = 0;
        fields = new LinkedTreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    boolean isFinished() {
        return cs == warc_first_final;
    }

    boolean isError() {
        return cs == warc_error;
    }

	void feed(ByteBuffer buffer) {
		int consumed = feed(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
		buffer.position(buffer.position() + consumed);
	}

    int feed(byte[] data) {
        return feed(data, 0, data.length);
    }

    int feed(byte[] data, int start, int length) {
        int p = start, pe = p + length, eof = pe;

        
// line 63 "WarcParser.java"
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
			if ( data[p] < _warc_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( data[p] > _warc_trans_keys[_mid] )
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
			if ( data[p] < _warc_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( data[p] > _warc_trans_keys[_mid+1] )
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
// line 5 "WarcParser.rl"
	{ buf.write(data[p]); }
	break;
	case 1:
// line 9 "WarcParser.rl"
	{ versionMajor = versionMajor * 10 + data[p] - '0'; }
	break;
	case 2:
// line 10 "WarcParser.rl"
	{ versionMinor = versionMinor * 10 + data[p] - '0'; }
	break;
	case 3:
// line 23 "WarcParser.rl"
	{ fieldName = slice(); }
	break;
	case 4:
// line 25 "WarcParser.rl"
	{ buf.write('\n'); }
	break;
	case 5:
// line 27 "WarcParser.rl"
	{ fields.put(fieldName, slice()); fieldName = null; }
	break;
	case 6:
// line 30 "WarcParser.rl"
	{ { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
// line 171 "WarcParser.java"
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

// line 84 "WarcParser.rl"

        return p - start;
    }

    private String slice() {
        try {
            return buf.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
            buf.reset();
        }
    }

    
// line 207 "WarcParser.java"
private static byte[] init__warc_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    2,    4,    0,    2,    5,    0
	};
}

private static final byte _warc_actions[] = init__warc_actions_0();


private static byte[] init__warc_key_offsets_0()
{
	return new byte [] {
	    0,    0,    1,    2,    3,    4,    5,    7,   10,   12,   15,   16,
	   32,   33,   49,   55,   56,   74,   80,   86
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
	   94,  122,    9,   13,   32,  127,    0,   31,   13,  127,    0,    8,
	   10,   31,    0
	};
}

private static final char _warc_trans_keys[] = init__warc_trans_keys_0();


private static byte[] init__warc_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    4,
	    1,    4,    4,    1,    6,    4,    2,    0
	};
}

private static final byte _warc_single_lengths[] = init__warc_single_lengths_0();


private static byte[] init__warc_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    1,    1,    1,    0,    6,
	    0,    6,    1,    0,    6,    1,    2,    0
	};
}

private static final byte _warc_range_lengths[] = init__warc_range_lengths_0();


private static byte[] init__warc_index_offsets_0()
{
	return new byte [] {
	    0,    0,    2,    4,    6,    8,   10,   12,   15,   17,   20,   22,
	   33,   35,   46,   52,   54,   67,   73,   78
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
	   23,   16,    1,    1,    1,   17,    1,    0
	};
}

private static final byte _warc_indicies[] = init__warc_indicies_0();


private static byte[] init__warc_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    4,    5,    6,    7,    8,    9,   10,   11,   12,
	   13,   19,   14,   14,   15,   18,   16,   17,   12,   13,   15,   18
	};
}

private static final byte _warc_trans_targs[] = init__warc_trans_targs_0();


private static byte[] init__warc_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    3,    0,    5,    0,    0,    0,
	    1,   13,    7,    0,    0,    1,    0,    0,   11,   18,    9,   15
	};
}

private static final byte _warc_trans_actions[] = init__warc_trans_actions_0();


static final int warc_start = 1;
static final int warc_first_final = 19;
static final int warc_error = 0;

static final int warc_en_header = 1;


// line 99 "WarcParser.rl"
}