
// line 1 "HttpParser.rl"
// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -

// line 103 "HttpParser.rl"


package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class HttpParser extends MessageParser {
    private int initialState;
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
        
// line 45 "HttpParser.java"
	{
	cs = http_start;
	}

// line 140 "HttpParser.rl"
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
        cs = initialState;
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

    /**
     * Configures the parser to read a HTTP request while rejecting deviations from the standard.
     */
    public void strictRequest() {
        cs = http_en_http_request;
        initialState = cs;
    }

    /**
     * Configures the parser to read a HTTP response while rejecting deviations from the standard.
     */
    public void strictResponse() {
        cs = http_en_http_response;
        initialState = cs;
    }

    /**
     * Configures the parser to read a HTTP request while allowing some deviations from the standard:
     * <ul>
     *   <li>The number of spaces in the request line may vary
     *   <li>Any character except space and newline is allowed in the target
     *   <li>Lines may end with LF instead of CRLF
     *   <li>Any byte except LF is allowed in header field values
     *   <li>Whitespace is allowed between the field name and colon
     * </ul>
     */
    public void lenientRequest() {
        cs = http_en_http_request_lenient;
        initialState = cs;
    }

    /**
     * Configures the parser to read a HTTP response while allowing some deviations from the standard:
     * <ul>
     *   <li>The number of spaces in the status line may vary
     *   <li>Lines may end with LF instead of CRLF
     *   <li>Any byte except LF is allowed in header field values
     *   <li>Whitespace is allowed between the field name and colon
     * </ul>
     */
    public void lenientResponse() {
        cs = http_en_http_response_lenient;
        initialState = cs;
    }

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) {
        int p = data.position();
        int pe = data.limit();

        
// line 152 "HttpParser.java"
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
			if ( ( (data.get(p) & 0xff)) < _http_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( (data.get(p) & 0xff)) > _http_trans_keys[_mid] )
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
			if ( ( (data.get(p) & 0xff)) < _http_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( (data.get(p) & 0xff)) > _http_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

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
    name = new String(buf, 0, bufPos, US_ASCII).trim();
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
// line 292 "HttpParser.java"
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

// line 241 "HttpParser.rl"

        position += p - data.position();
        data.position(p);
    }

    public void parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        parse(channel, buffer, null);
    }

    void parse(ReadableByteChannel channel, ByteBuffer buffer, WritableByteChannel copyTo) throws IOException {
        while (true) {
            ByteBuffer copy = buffer.duplicate();
            long buffOffset = buffer.position() - position;
            parse(buffer);
            if (copyTo != null) {
                copy.limit(buffer.position());
                copyTo.write(copy);
            }
            if (isFinished()) {
                break;
            }
            if (isError()) {
                throw new ParsingException("invalid HTTP message at byte position " + position + ": "
                        + getErrorContext(buffer.duplicate(), (int) (buffOffset + position), 40));
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

    
// line 353 "HttpParser.java"
private static byte[] init__http_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   10,    1,
	   11,    2,    4,    0,    2,    9,    0,    2,   11,    0,    2,   11,
	    8,    2,   11,   10
	};
}

private static final byte _http_actions[] = init__http_actions_0();


private static short[] init__http_key_offsets_0()
{
	return new short [] {
	    0,    0,    1,    2,    3,    4,    5,    7,    8,   10,   11,   14,
	   16,   18,   21,   26,   29,   33,   37,   41,   46,   48,   52,   56,
	   61,   65,   69,   71,   78,   93,  109,  121,  134,  135,  136,  137,
	  138,  139,  141,  142,  144,  145,  146,  162,  163,  179,  186,  187,
	  205,  212,  219,  226,  227,  228,  229,  230,  231,  233,  234,  236,
	  237,  239,  241,  243,  244,  250,  251,  267,  268,  284,  291,  292,
	  310,  317,  324,  331,  346,  362,  365,  368,  373,  376,  380,  384,
	  388,  393,  395,  399,  403,  408,  412,  416,  418,  422,  423,  424,
	  425,  426,  428,  429,  431,  434,  434,  434,  434
	};
}

private static final short _http_key_offsets[] = init__http_key_offsets_0();


private static char[] init__http_trans_keys_0()
{
	return new char [] {
	   72,   84,   84,   80,   47,   48,   57,   46,   48,   57,   32,   32,
	   48,   57,   48,   57,   48,   57,   10,   13,   32,    9,   10,   13,
	   32,   58,   10,   13,   58,    9,   10,   13,   32,    9,   10,   13,
	   32,    9,   10,   13,   32,    9,   10,   13,   32,   58,   10,   13,
	    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,
	   58,    9,   10,   13,   32,    9,   10,   13,   32,   10,   13,    9,
	   10,   13,   32,  126,  128,  255,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,   32,   33,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,   33,   61,   95,  126,   36,   59,   63,   90,   97,  122,  128,
	  255,   32,   33,   61,   95,  126,   36,   59,   63,   90,   97,  122,
	  128,  255,   72,   84,   84,   80,   47,   48,   57,   46,   48,   57,
	   13,   10,   13,   33,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,   10,   33,   58,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   13,   32,   33,  126,  128,  255,   10,    9,   13,   32,   33,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   13,   32,   33,  126,  128,  255,    9,   13,   32,   33,
	  126,  128,  255,    9,   13,   32,   33,  126,  128,  255,   72,   84,
	   84,   80,   47,   48,   57,   46,   48,   57,   32,   48,   57,   48,
	   57,   48,   57,   32,    9,   13,   32,  126,  128,  255,   10,   13,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   10,   33,   58,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,   33,
	  126,  128,  255,   10,    9,   13,   32,   33,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,
	   32,   33,  126,  128,  255,    9,   13,   32,   33,  126,  128,  255,
	    9,   13,   32,   33,  126,  128,  255,   33,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   32,   33,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   10,   13,   32,   10,   13,   32,    9,   10,   13,   32,
	   58,   10,   13,   58,    9,   10,   13,   32,    9,   10,   13,   32,
	    9,   10,   13,   32,    9,   10,   13,   32,   58,   10,   13,    9,
	   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,   58,
	    9,   10,   13,   32,    9,   10,   13,   32,   10,   13,   10,   13,
	   32,   72,   84,   84,   80,   47,   48,   57,   46,   48,   57,   10,
	   13,   32,    0
	};
}

private static final char _http_trans_keys[] = init__http_trans_keys_0();


private static byte[] init__http_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    0,
	    0,    3,    5,    3,    4,    4,    4,    5,    2,    4,    4,    5,
	    4,    4,    2,    3,    3,    4,    4,    5,    1,    1,    1,    1,
	    1,    0,    1,    0,    1,    1,    4,    1,    4,    3,    1,    6,
	    3,    3,    3,    1,    1,    1,    1,    1,    0,    1,    0,    1,
	    0,    0,    0,    1,    2,    1,    4,    1,    4,    3,    1,    6,
	    3,    3,    3,    3,    4,    3,    3,    5,    3,    4,    4,    4,
	    5,    2,    4,    4,    5,    4,    4,    2,    4,    1,    1,    1,
	    1,    0,    1,    0,    3,    0,    0,    0,    0
	};
}

private static final byte _http_single_lengths[] = init__http_single_lengths_0();


private static byte[] init__http_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    0,    1,    0,    1,    1,
	    1,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    2,    6,    6,    4,    4,    0,    0,    0,    0,
	    0,    1,    0,    1,    0,    0,    6,    0,    6,    2,    0,    6,
	    2,    2,    2,    0,    0,    0,    0,    0,    1,    0,    1,    0,
	    1,    1,    1,    0,    2,    0,    6,    0,    6,    2,    0,    6,
	    2,    2,    2,    6,    6,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    1,    0,    1,    0,    0,    0,    0,    0
	};
}

private static final byte _http_range_lengths[] = init__http_range_lengths_0();


private static short[] init__http_index_offsets_0()
{
	return new short [] {
	    0,    0,    2,    4,    6,    8,   10,   12,   14,   16,   18,   21,
	   23,   25,   29,   35,   39,   44,   49,   54,   60,   63,   68,   73,
	   79,   84,   89,   92,   98,  108,  119,  128,  138,  140,  142,  144,
	  146,  148,  150,  152,  154,  156,  158,  169,  171,  182,  188,  190,
	  203,  209,  215,  221,  223,  225,  227,  229,  231,  233,  235,  237,
	  239,  241,  243,  245,  247,  252,  254,  265,  267,  278,  284,  286,
	  299,  305,  311,  317,  327,  338,  342,  346,  352,  356,  361,  366,
	  371,  377,  380,  385,  390,  396,  401,  406,  409,  414,  416,  418,
	  420,  422,  424,  426,  428,  432,  433,  434,  435
	};
}

private static final short _http_index_offsets[] = init__http_index_offsets_0();


private static byte[] init__http_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    0,    4,    0,    5,    0,    6,    0,    7,    0,
	    8,    0,    9,    0,   10,    0,   10,   11,    0,   12,    0,   13,
	    0,   14,   26,   27,    0,    0,  101,   20,    0,   16,   15,    0,
	    0,   16,   15,   16,   19,   21,   16,   17,   18,   19,   21,   18,
	   17,   18,   19,   21,   18,   17,    0,  101,   20,    0,   16,   15,
	  101,   20,    0,   22,   23,   25,   22,   17,   22,    0,   22,   22,
	   17,   24,  101,   20,   24,   16,   15,   24,   19,   21,   24,   17,
	   22,   19,   25,   22,   17,   14,   26,    0,   27,   14,   26,   27,
	   27,    0,   29,   29,   29,   29,   29,   29,   29,   29,   29,    0,
	   30,   29,   29,   29,   29,   29,   29,   29,   29,   29,    0,   31,
	   31,   31,   31,   31,   31,   31,   31,    0,   32,   31,   31,   31,
	   31,   31,   31,   31,   31,    0,   33,    0,   34,    0,   35,    0,
	   36,    0,   37,    0,   38,    0,   39,    0,   40,    0,   41,    0,
	   42,    0,   43,   44,   44,   44,   44,   44,   44,   44,   44,   44,
	    0,  102,    0,   44,   45,   44,   44,   44,   44,   44,   44,   44,
	   44,    0,   45,   46,   45,   49,   49,    0,   47,    0,   48,   43,
	   48,   44,   44,   44,   44,   44,   44,   44,   44,   44,    0,   48,
	   46,   48,   49,   49,    0,   50,   46,   50,   49,   49,    0,   50,
	   46,   50,   49,   49,    0,   52,    0,   53,    0,   54,    0,   55,
	    0,   56,    0,   57,    0,   58,    0,   59,    0,   60,    0,   61,
	    0,   62,    0,   63,    0,   64,    0,   64,   65,   64,   64,    0,
	   66,    0,   67,   68,   68,   68,   68,   68,   68,   68,   68,   68,
	    0,  103,    0,   68,   69,   68,   68,   68,   68,   68,   68,   68,
	   68,    0,   69,   70,   69,   73,   73,    0,   71,    0,   72,   67,
	   72,   68,   68,   68,   68,   68,   68,   68,   68,   68,    0,   72,
	   70,   72,   73,   73,    0,   74,   70,   74,   73,   73,    0,   74,
	   70,   74,   73,   73,    0,   76,   76,   76,   76,   76,   76,   76,
	   76,   76,    0,   77,   76,   76,   76,   76,   76,   76,   76,   76,
	   76,    0,    0,    0,   77,   78,   79,   91,   92,   78,    0,  104,
	   85,    0,   81,   80,    0,    0,   81,   80,   81,   84,   86,   81,
	   82,   83,   84,   86,   83,   82,   83,   84,   86,   83,   82,    0,
	  104,   85,    0,   81,   80,  104,   85,    0,   87,   88,   90,   87,
	   82,   87,    0,   87,   87,   82,   89,  104,   85,   89,   81,   80,
	   89,   84,   86,   89,   82,   87,   84,   90,   87,   82,   79,   91,
	    0,   79,   91,   92,   93,    0,   94,    0,   95,    0,   96,    0,
	   97,    0,   98,    0,   99,    0,  100,    0,   79,   91,  100,    0,
	    0,    0,    0,    0,    0
	};
}

private static final byte _http_trans_targs[] = init__http_trans_targs_0();


private static byte[] init__http_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    3,    0,
	    0,    0,    5,    0,    0,    0,    0,    7,    0,    7,    0,    7,
	    0,    0,    0,    0,    0,    0,   17,    0,    0,   21,    1,    0,
	    0,   21,    1,    0,    0,    1,    0,    1,   25,    9,   25,   25,
	    1,    1,    0,    1,    1,    1,    0,   34,   23,    0,   37,   31,
	   17,    0,    0,    1,    0,    1,    1,    1,    1,    0,    1,    1,
	    1,    0,   34,   23,    0,   37,   31,    0,   19,   28,    0,   28,
	    1,    0,    1,    1,    1,    0,    0,    0,    1,   13,   13,    1,
	    1,    0,    1,    1,    1,    1,    1,    1,    1,    1,    1,    0,
	   11,    1,    1,    1,    1,    1,    1,    1,    1,    1,    0,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,   15,    1,    1,    1,
	    1,    1,    1,    1,    1,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    3,    0,    0,    0,    5,    0,    0,    0,
	    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    0,   17,    0,    1,   21,    1,    1,    1,    1,    1,    1,    1,
	    1,    0,    0,    0,    0,    1,    1,    0,    0,    0,    0,   23,
	    0,   31,   31,   31,   31,   31,   31,   31,   31,   31,    0,    0,
	   19,    0,   28,   28,    0,   25,    9,   25,    1,    1,    0,    1,
	    0,    1,    1,    1,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    3,    0,    0,    0,    5,    0,    0,    0,    7,
	    0,    7,    0,    7,    0,    0,    0,    1,   13,    1,    1,    0,
	    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    0,   17,    0,    1,   21,    1,    1,    1,    1,    1,    1,    1,
	    1,    0,    0,    0,    0,    1,    1,    0,    0,    0,    0,   23,
	    0,   31,   31,   31,   31,   31,   31,   31,   31,   31,    0,    0,
	   19,    0,   28,   28,    0,   25,    9,   25,    1,    1,    0,    1,
	    0,    1,    1,    1,    0,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    0,   11,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    0,    0,    0,    0,    1,   15,   15,   15,    1,    0,   17,
	    0,    0,   21,    1,    0,    0,   21,    1,    0,    0,    1,    0,
	    1,   25,    9,   25,   25,    1,    1,    0,    1,    1,    1,    0,
	   34,   23,    0,   37,   31,   17,    0,    0,    1,    0,    1,    1,
	    1,    1,    0,    1,    1,    1,    0,   34,   23,    0,   37,   31,
	    0,   19,   28,    0,   28,    1,    0,    1,    1,    1,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    3,    0,    0,    0,    5,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0
	};
}

private static final byte _http_trans_actions[] = init__http_trans_actions_0();


static final int http_start = 1;
static final int http_first_final = 101;
static final int http_error = 0;

static final int http_en_http_request = 28;
static final int http_en_http_response = 51;
static final int http_en_http_request_lenient = 75;
static final int http_en_http_response_lenient = 1;


// line 281 "HttpParser.rl"
}