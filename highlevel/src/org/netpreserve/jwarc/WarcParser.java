
// line 1 "WarcParser.rl"
// recompile: ragel -J WarcParser.rl -o WarcParser.java
// diagram:   ragel -Vp WarcParser.rl | dot -TPng | feh -

package org.netpreserve.jwarc;

import javax.annotation.Generated;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.US_ASCII;


// line 68 "WarcParser.rl"


/**
 * Low-level WARC record parser.
 * <p>
 * Unless you're doing something advanced (like non-blocking IO) you should use the higher-level {@link WarcReader}
 * class instead.
 */
public class WarcParser {
    private int entryState;
    private int cs;
    private long position;
    private byte[] buf = new byte[256];
    private int bufPos;
    private int endOfText;
    private int major;
    private int minor;
    private String name;
    private Map<String,List<String>> headerMap;

    public static WarcParser newWarcFieldsParser() {
        return new WarcParser(warc_en_warc_fields);
    }

    public WarcParser() {
        this(warc_start);
    }

    private WarcParser(int entryState) {
        this.entryState = entryState;
        reset();
    }

    public void reset() {
        cs = entryState;
        position = 0;
        bufPos = 0;
        endOfText = 0;
        major = 0;
        minor = 0;
        name = null;
        headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (buf.length > 4096) {
            buf = new byte[4096];
        }
    }

    public boolean isFinished() {
        return cs >= warc_first_final;
    }

    public boolean isError() {
        return cs == warc_error;
    }

    @Generated("Ragel")
    public void parse(ByteBuffer data) {
        int p = data.position();
        int pe = data.limit();

        
// line 82 "WarcParser.java"
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
// line 22 "WarcParser.rl"
	{ push(data.get(p)); }
	break;
	case 1:
// line 23 "WarcParser.rl"
	{ if (bufPos > 0) push((byte)' '); }
	break;
	case 2:
// line 24 "WarcParser.rl"
	{ major = major * 10 + data.get(p) - '0'; }
	break;
	case 3:
// line 25 "WarcParser.rl"
	{ minor = minor * 10 + data.get(p) - '0'; }
	break;
	case 4:
// line 26 "WarcParser.rl"
	{ endOfText = bufPos; }
	break;
	case 5:
// line 28 "WarcParser.rl"
	{
    name = new String(buf, 0, bufPos, US_ASCII);
    bufPos = 0;
}
	break;
	case 6:
// line 33 "WarcParser.rl"
	{
    String value = new String(buf, 0, endOfText, UTF_8);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}
	break;
	case 7:
// line 66 "WarcParser.rl"
	{ { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
// line 202 "WarcParser.java"
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

// line 129 "WarcParser.rl"

        position += p - data.position();
        data.position(p);
    }

    public boolean parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (true) {
            parse(buffer);
            if (isFinished()) {
                return true;
            }
            if (isError()) throw new ParsingException("invalid WARC record");
            buffer.compact();
            int n = channel.read(buffer);
            if (n < 0) {
                if (position > 0) {
                    throw new EOFException();
                }
                return false;
            }
            buffer.flip();
        }
    }

    private void push(byte b) {
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    public Headers headers() {
        return new Headers(headerMap);
    }

    public ProtocolVersion version() {
        return new ProtocolVersion("WARC", major, minor);
    }

    public long position() {
        return position;
    }

    
// line 267 "WarcParser.java"
private static byte[] init__warc_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    2,    1,    0,    2,    4,    0,    2,
	    6,    0
	};
}

private static final byte _warc_actions[] = init__warc_actions_0();


private static short[] init__warc_key_offsets_0()
{
	return new short [] {
	    0,    0,    1,    2,    3,    4,    5,    7,   10,   12,   15,   16,
	   32,   33,   49,   55,   56,   74,   80,   86,   92,  108,  109,  125,
	  131,  132,  150,  156,  162,  168,  168
	};
}

private static final short _warc_key_offsets[] = init__warc_key_offsets_0();


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
	    0,   31,    9,   13,   32,  127,    0,   31,   13,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	   10,   33,   58,  124,  126,   35,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,    9,   13,   32,  127,    0,   31,   10,
	    9,   13,   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,    9,   13,   32,  127,    0,   31,
	    9,   13,   32,  127,    0,   31,    9,   13,   32,  127,    0,   31,
	    0
	};
}

private static final char _warc_trans_keys[] = init__warc_trans_keys_0();


private static byte[] init__warc_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    4,
	    1,    4,    4,    1,    6,    4,    4,    4,    4,    1,    4,    4,
	    1,    6,    4,    4,    4,    0,    0
	};
}

private static final byte _warc_single_lengths[] = init__warc_single_lengths_0();


private static byte[] init__warc_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    1,    1,    1,    0,    6,
	    0,    6,    1,    0,    6,    1,    1,    1,    6,    0,    6,    1,
	    0,    6,    1,    1,    1,    0,    0
	};
}

private static final byte _warc_range_lengths[] = init__warc_range_lengths_0();


private static short[] init__warc_index_offsets_0()
{
	return new short [] {
	    0,    0,    2,    4,    6,    8,   10,   12,   15,   17,   20,   22,
	   33,   35,   46,   52,   54,   67,   73,   79,   85,   96,   98,  109,
	  115,  117,  130,  136,  142,  148,  149
	};
}

private static final short _warc_index_offsets[] = init__warc_index_offsets_0();


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
	   17,   27,   28,   28,   28,   28,   28,   28,   28,   28,   28,    1,
	   29,    1,   28,   30,   28,   28,   28,   28,   28,   28,   28,   28,
	    1,   31,   32,   31,    1,    1,   33,   34,    1,   35,   36,   35,
	   37,   37,   37,   37,   37,   37,   37,   37,   37,    1,   35,   38,
	   35,    1,    1,   39,   40,   41,   40,    1,    1,   33,   42,   32,
	   42,    1,    1,   33,    1,    1,    0
	};
}

private static final byte _warc_indicies[] = init__warc_indicies_0();


private static byte[] init__warc_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    4,    5,    6,    7,    8,    9,   10,   11,   12,
	   13,   29,   14,   14,   15,   18,   16,   17,   12,   13,   15,   18,
	   19,   15,   19,   21,   22,   30,   23,   23,   24,   27,   25,   26,
	   21,   22,   24,   27,   28,   24,   28
	};
}

private static final byte _warc_trans_targs[] = init__warc_trans_targs_0();


private static byte[] init__warc_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    5,    0,    7,    0,    0,    0,
	    1,   15,   11,    0,    0,    1,    0,    0,   13,   23,    3,   17,
	   20,    9,    1,    0,    1,    0,   11,    0,    0,    1,    0,    0,
	   13,   23,    3,   17,   20,    9,    1
	};
}

private static final byte _warc_trans_actions[] = init__warc_trans_actions_0();


static final int warc_start = 1;
static final int warc_first_final = 29;
static final int warc_error = 0;

static final int warc_en_warc_fields = 20;
static final int warc_en_warc_header = 1;


// line 173 "WarcParser.rl"
}