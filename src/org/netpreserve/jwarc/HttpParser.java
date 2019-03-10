
// line 1 "HttpParser.rl"
// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -

// line 83 "HttpParser.rl"


package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class HttpParser {
    private int cs;
    private long position;
    private boolean finished;
    private byte[] buf = new byte[256];
    private int bufPos = 0;
    private int endOfText;
    private int major;
    private int minor;
    private int status;
    private String reason;
    private String method;
    private String target;
    private String name;
    private Map<String,List<String>> headerMap;

	public HttpParser() {
        reset();
    }

    public void reset() {
        
// line 43 "HttpParser.java"
	{
	cs = http_start;
	}

// line 118 "HttpParser.rl"
        bufPos = 0;
        if (buf.length > 8192) {
            buf = new byte[256]; // if our buffer grew really big release it
        }
        major = 0;
        minor = 0;
        status = 0;
        reason = null;
        method = null;
        target = null;
        name = null;
        headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        endOfText = 0;
        position = 0;
        finished = false;
    }

    public MessageHeaders headers() {
        return new MessageHeaders(headerMap);
    }

    public MessageVersion version() {
        return new MessageVersion("HTTP", major, minor);
    }

    public int status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public String target() {
        return target;
    }

    public String method() {
        return method;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isError() {
        return cs == http_error;
    }

    public void requestOnly() {
        cs = http_en_http_request;
    }

    public void responseOnly() {
        cs = http_en_http_response;
    }

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) {
        int p = data.position();
        int pe = data.limit();

        
// line 112 "HttpParser.java"
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
	_keys = _http_key_offsets[cs];
	_trans = _http_index_offsets[cs];
	_klen = _http_single_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + _klen - 1;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + ((_upper-_lower) >> 1);
			if ( ( data.get(p)) < _http_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( data.get(p)) > _http_trans_keys[_mid] )
				_lower = _mid + 1;
			else {
				_trans += (_mid - _keys);
				break _match;
			}
		}
		_keys += _klen;
		_trans += _klen;
	}

	_klen = _http_range_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + (_klen<<1) - 2;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + (((_upper-_lower) >> 1) & ~1);
			if ( ( data.get(p)) < _http_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( data.get(p)) > _http_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

	_trans = _http_indicies[_trans];
	cs = _http_trans_targs[_trans];

	if ( _http_trans_actions[_trans] != 0 ) {
		_acts = _http_trans_actions[_trans];
		_nacts = (int) _http_actions[_acts++];
		while ( _nacts-- > 0 )
	{
			switch ( _http_actions[_acts++] )
			{
	case 0:
// line 9 "HttpParser.rl"
	{ push(data.get(p)); }
	break;
	case 1:
// line 11 "HttpParser.rl"
	{ major = major * 10 + data.get(p) - '0'; }
	break;
	case 2:
// line 12 "HttpParser.rl"
	{ minor = minor * 10 + data.get(p) - '0'; }
	break;
	case 3:
// line 13 "HttpParser.rl"
	{ status = status * 10 + data.get(p) - '0'; }
	break;
	case 4:
// line 14 "HttpParser.rl"
	{ endOfText = bufPos; }
	break;
	case 5:
// line 15 "HttpParser.rl"
	{ method = new String(buf, 0, bufPos, US_ASCII); bufPos = 0; }
	break;
	case 6:
// line 16 "HttpParser.rl"
	{ reason = new String(buf, 0, bufPos, ISO_8859_1); bufPos = 0; }
	break;
	case 7:
// line 17 "HttpParser.rl"
	{ target = new String(buf, 0, bufPos, ISO_8859_1); bufPos = 0; }
	break;
	case 8:
// line 18 "HttpParser.rl"
	{ finished = true; }
	break;
	case 9:
// line 20 "HttpParser.rl"
	{
    if (bufPos > 0) {
        bufPos = endOfText;
        push((byte)' ');
    }
}
	break;
	case 10:
// line 27 "HttpParser.rl"
	{
    name = new String(buf, 0, bufPos, US_ASCII);
    bufPos = 0;
}
	break;
	case 11:
// line 32 "HttpParser.rl"
	{
    String value = new String(buf, 0, endOfText, ISO_8859_1);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}
	break;
// line 253 "HttpParser.java"
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

// line 181 "HttpParser.rl"

        position += p - data.position();
        data.position(p);
    }

    public void parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (true) {
            parse(buffer);
            if (isFinished()) {
                break;
            }
            if (isError()) {
                throw new ParsingException("invalid HTTP message at byte position " + position);
            }
            buffer.compact();
            int n = channel.read(buffer);
            if (n < 0) throw new EOFException("state=" + cs);
            buffer.flip();
        }
    }

    private void push(byte b) {
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    
// line 303 "HttpParser.java"
private static byte[] init__http_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   10,    1,
	   11,    2,    4,    0,    2,    9,    0,    2,   11,    0
	};
}

private static final byte _http_actions[] = init__http_actions_0();


private static short[] init__http_key_offsets_0()
{
	return new short [] {
	    0,    0,    1,    2,    3,    4,    5,    7,    8,   10,   11,   13,
	   15,   17,   18,   24,   25,   41,   42,   58,   65,   66,   84,   91,
	   98,  105,  120,  136,  148,  161,  162,  163,  164,  165,  166,  168,
	  169,  171,  172,  173,  189,  190,  206,  213,  214,  232,  239,  246,
	  253,  253
	};
}

private static final short _http_key_offsets[] = init__http_key_offsets_0();


private static char[] init__http_trans_keys_0()
{
	return new char [] {
	   72,   84,   84,   80,   47,   48,   57,   46,   48,   57,   32,   48,
	   57,   48,   57,   48,   57,   32,    9,   13,   32,  126,  128,  255,
	   10,   13,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,   10,   33,   58,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,
	   32,   33,  126,  128,  255,   10,    9,   13,   32,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   13,   32,   33,  126,  128,  255,    9,   13,   32,   33,  126,
	  128,  255,    9,   13,   32,   33,  126,  128,  255,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   33,   61,   95,  126,   36,   59,   63,   90,
	   97,  122,  128,  255,   32,   33,   61,   95,  126,   36,   59,   63,
	   90,   97,  122,  128,  255,   72,   84,   84,   80,   47,   48,   57,
	   46,   48,   57,   13,   10,   13,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,   33,   58,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,    9,   13,   32,   33,  126,  128,  255,   10,    9,   13,
	   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   13,   32,   33,  126,  128,  255,    9,
	   13,   32,   33,  126,  128,  255,    9,   13,   32,   33,  126,  128,
	  255,    0
	};
}

private static final char _http_trans_keys[] = init__http_trans_keys_0();


private static byte[] init__http_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    0,    0,
	    0,    1,    2,    1,    4,    1,    4,    3,    1,    6,    3,    3,
	    3,    3,    4,    4,    5,    1,    1,    1,    1,    1,    0,    1,
	    0,    1,    1,    4,    1,    4,    3,    1,    6,    3,    3,    3,
	    0,    0
	};
}

private static final byte _http_single_lengths[] = init__http_single_lengths_0();


private static byte[] init__http_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    0,    1,    0,    1,    1,
	    1,    0,    2,    0,    6,    0,    6,    2,    0,    6,    2,    2,
	    2,    6,    6,    4,    4,    0,    0,    0,    0,    0,    1,    0,
	    1,    0,    0,    6,    0,    6,    2,    0,    6,    2,    2,    2,
	    0,    0
	};
}

private static final byte _http_range_lengths[] = init__http_range_lengths_0();


private static short[] init__http_index_offsets_0()
{
	return new short [] {
	    0,    0,    2,    4,    6,    8,   10,   12,   14,   16,   18,   20,
	   22,   24,   26,   31,   33,   44,   46,   57,   63,   65,   78,   84,
	   90,   96,  106,  117,  126,  136,  138,  140,  142,  144,  146,  148,
	  150,  152,  154,  156,  167,  169,  180,  186,  188,  201,  207,  213,
	  219,  220
	};
}

private static final short _http_index_offsets[] = init__http_index_offsets_0();


private static byte[] init__http_indicies_0()
{
	return new byte [] {
	    0,    1,    2,    1,    3,    1,    4,    1,    5,    1,    6,    1,
	    7,    1,    8,    1,    9,    1,   10,    1,   11,    1,   12,    1,
	   13,    1,   14,   15,   14,   14,    1,   16,    1,   17,   18,   18,
	   18,   18,   18,   18,   18,   18,   18,    1,   19,    1,   18,   20,
	   18,   18,   18,   18,   18,   18,   18,   18,    1,   21,   22,   21,
	   23,   23,    1,   24,    1,   25,   26,   25,   27,   27,   27,   27,
	   27,   27,   27,   27,   27,    1,   25,   28,   25,   29,   29,    1,
	   30,   31,   30,   23,   23,    1,   32,   22,   32,   23,   23,    1,
	   33,   33,   33,   33,   33,   33,   33,   33,   33,    1,   34,   33,
	   33,   33,   33,   33,   33,   33,   33,   33,    1,   35,   35,   35,
	   35,   35,   35,   35,   35,    1,   36,   35,   35,   35,   35,   35,
	   35,   35,   35,    1,   37,    1,   38,    1,   39,    1,   40,    1,
	   41,    1,   42,    1,   43,    1,   44,    1,   45,    1,   46,    1,
	   47,   48,   48,   48,   48,   48,   48,   48,   48,   48,    1,   49,
	    1,   48,   50,   48,   48,   48,   48,   48,   48,   48,   48,    1,
	   51,   52,   51,   53,   53,    1,   54,    1,   55,   56,   55,   57,
	   57,   57,   57,   57,   57,   57,   57,   57,    1,   55,   58,   55,
	   59,   59,    1,   60,   61,   60,   53,   53,    1,   62,   52,   62,
	   53,   53,    1,    1,    1,    0
	};
}

private static final byte _http_indicies[] = init__http_indicies_0();


private static byte[] init__http_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    4,    5,    6,    7,    8,    9,   10,   11,   12,
	   13,   14,   14,   15,   16,   17,   18,   48,   19,   19,   20,   23,
	   21,   22,   17,   18,   20,   23,   24,   20,   24,   26,   27,   28,
	   29,   30,   31,   32,   33,   34,   35,   36,   37,   38,   39,   40,
	   41,   49,   42,   42,   43,   46,   44,   45,   40,   41,   43,   46,
	   47,   43,   47
	};
}

private static final byte _http_trans_targs[] = init__http_trans_targs_0();


private static byte[] init__http_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    3,    0,    5,    0,    7,    7,
	    7,    0,    1,   13,    0,    0,    1,   17,   21,    0,    0,    1,
	    0,    0,   23,   31,   19,   28,   25,    9,    1,    1,   11,    1,
	   15,    0,    0,    0,    0,    0,    3,    0,    5,    0,    0,    0,
	    1,   17,   21,    0,    0,    1,    0,    0,   23,   31,   19,   28,
	   25,    9,    1
	};
}

private static final byte _http_trans_actions[] = init__http_trans_actions_0();


static final int http_start = 1;
static final int http_first_final = 48;
static final int http_error = 0;

static final int http_en_http_request = 25;
static final int http_en_http_response = 1;


// line 210 "HttpParser.rl"
}