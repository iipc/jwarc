
// line 1 "HttpParser.rl"
// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -

// line 110 "HttpParser.rl"


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

// line 147 "HttpParser.rl"
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

    /**
     * Runs the parser on a buffer of data. Passing null as the buffer indicates the end of input.
     */
    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) {
        int p;
        int pe;
        int eof;

        if (data == null) {
            p = 0;
            pe = 0;
            eof = 0;
        } else {
            p = data.position();
            pe = data.limit();
            eof = -1;
        }

        
// line 166 "HttpParser.java"
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
	{ finished = true; { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
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
// line 306 "HttpParser.java"
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
	int __acts = _http_eof_actions[cs];
	int __nacts = (int) _http_actions[__acts++];
	while ( __nacts-- > 0 ) {
		switch ( _http_actions[__acts++] ) {
	case 4:
// line 14 "HttpParser.rl"
	{ endOfText = bufPos; }
	break;
	case 8:
// line 18 "HttpParser.rl"
	{ finished = true; { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
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
	case 11:
// line 32 "HttpParser.rl"
	{
    String value = new String(buf, 0, endOfText, ISO_8859_1);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}
	break;
// line 353 "HttpParser.java"
		}
	}
	}

case 5:
	}
	break; }
	}

// line 262 "HttpParser.rl"

        if (data != null) {
            position += p - data.position();
            data.position(p);
        }
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
            buffer.flip();
            if (n < 0) {
                parse(null);
                break;
            }
        }
    }

    private void push(byte b) {
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    
// line 409 "HttpParser.java"
private static byte[] init__http_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   10,    1,
	   11,    2,    4,    0,    2,    9,    0,    2,   11,    0,    2,   11,
	    8,    2,   11,   10,    3,    4,   11,    8,    3,    9,   11,    8
	};
}

private static final byte _http_actions[] = init__http_actions_0();


private static short[] init__http_key_offsets_0()
{
	return new short [] {
	    0,    0,    1,    2,    3,    4,    6,    9,   11,   13,   16,   19,
	   23,   27,   29,   31,   38,   40,   42,   44,   45,   60,   76,   88,
	  101,  102,  103,  104,  105,  106,  108,  109,  111,  112,  113,  129,
	  130,  146,  153,  154,  172,  179,  186,  193,  194,  195,  196,  197,
	  198,  200,  201,  203,  204,  206,  208,  210,  211,  217,  218,  234,
	  235,  251,  258,  259,  277,  284,  291,  298,  313,  329,  332,  335,
	  338,  342,  346,  348,  350,  354,  355,  356,  357,  358,  360,  361,
	  363,  366,  371,  375,  379,  383,  388,  392,  392,  392,  392,  397,
	  401,  405,  409,  414,  418
	};
}

private static final short _http_key_offsets[] = init__http_key_offsets_0();


private static char[] init__http_trans_keys_0()
{
	return new char [] {
	   72,   84,   84,   80,   32,   47,   32,   48,   57,   48,   57,   48,
	   57,   10,   13,   32,   10,   13,   58,    9,   10,   13,   32,    9,
	   10,   13,   32,   10,   13,   10,   13,    9,   10,   13,   32,  126,
	  128,  255,   48,   57,   32,   46,   48,   57,   32,   33,  124,  126,
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
	  255,   72,   84,   84,   80,   47,   48,   57,   46,   48,   57,   32,
	   48,   57,   48,   57,   48,   57,   32,    9,   13,   32,  126,  128,
	  255,   10,   13,   33,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,   10,   33,   58,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   13,   32,   33,  126,  128,  255,   10,    9,   13,   32,   33,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   13,   32,   33,  126,  128,  255,    9,   13,   32,   33,
	  126,  128,  255,    9,   13,   32,   33,  126,  128,  255,   33,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,   10,   13,   32,   10,   13,   32,   10,
	   13,   58,    9,   10,   13,   32,    9,   10,   13,   32,   10,   13,
	   10,   13,   10,   13,   32,   72,   84,   84,   80,   47,   48,   57,
	   46,   48,   57,   10,   13,   32,    9,   10,   13,   32,   58,    9,
	   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,    9,
	   10,   13,   32,   58,    9,   10,   13,   32,    9,   10,   13,   32,
	   58,    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,
	   32,    9,   10,   13,   32,   58,    9,   10,   13,   32,    0
	};
}

private static final char _http_trans_keys[] = init__http_trans_keys_0();


private static byte[] init__http_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    2,    1,    0,    0,    3,    3,    4,
	    4,    2,    2,    3,    0,    2,    0,    1,    3,    4,    4,    5,
	    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    4,    1,
	    4,    3,    1,    6,    3,    3,    3,    1,    1,    1,    1,    1,
	    0,    1,    0,    1,    0,    0,    0,    1,    2,    1,    4,    1,
	    4,    3,    1,    6,    3,    3,    3,    3,    4,    3,    3,    3,
	    4,    4,    2,    2,    4,    1,    1,    1,    1,    0,    1,    0,
	    3,    5,    4,    4,    4,    5,    4,    0,    0,    0,    5,    4,
	    4,    4,    5,    4,    0
	};
}

private static final byte _http_single_lengths[] = init__http_single_lengths_0();


private static byte[] init__http_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    1,    1,    0,    0,    0,
	    0,    0,    0,    2,    1,    0,    1,    0,    6,    6,    4,    4,
	    0,    0,    0,    0,    0,    1,    0,    1,    0,    0,    6,    0,
	    6,    2,    0,    6,    2,    2,    2,    0,    0,    0,    0,    0,
	    1,    0,    1,    0,    1,    1,    1,    0,    2,    0,    6,    0,
	    6,    2,    0,    6,    2,    2,    2,    6,    6,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    1,    0,    1,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0
	};
}

private static final byte _http_range_lengths[] = init__http_range_lengths_0();


private static short[] init__http_index_offsets_0()
{
	return new short [] {
	    0,    0,    2,    4,    6,    8,   11,   14,   16,   18,   22,   26,
	   31,   36,   39,   42,   48,   50,   53,   55,   57,   67,   78,   87,
	   97,   99,  101,  103,  105,  107,  109,  111,  113,  115,  117,  128,
	  130,  141,  147,  149,  162,  168,  174,  180,  182,  184,  186,  188,
	  190,  192,  194,  196,  198,  200,  202,  204,  206,  211,  213,  224,
	  226,  237,  243,  245,  258,  264,  270,  276,  286,  297,  301,  305,
	  309,  314,  319,  322,  325,  330,  332,  334,  336,  338,  340,  342,
	  344,  348,  354,  359,  364,  369,  375,  380,  381,  382,  383,  389,
	  394,  399,  404,  410,  415
	};
}

private static final short _http_index_offsets[] = init__http_index_offsets_0();


private static byte[] init__http_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    0,    4,    0,    5,    0,    6,   16,    0,    6,
	    7,    0,    8,    0,    9,    0,   85,   14,   15,    0,    0,    0,
	   86,   10,   12,   89,   11,   12,   87,   12,    0,   12,   12,   87,
	   91,   13,    0,   85,   14,    0,   15,   85,   14,   15,   15,    0,
	   17,    0,    6,   18,    0,   19,    0,    6,    0,   21,   21,   21,
	   21,   21,   21,   21,   21,   21,    0,   22,   21,   21,   21,   21,
	   21,   21,   21,   21,   21,    0,   23,   23,   23,   23,   23,   23,
	   23,   23,    0,   24,   23,   23,   23,   23,   23,   23,   23,   23,
	    0,   25,    0,   26,    0,   27,    0,   28,    0,   29,    0,   30,
	    0,   31,    0,   32,    0,   33,    0,   34,    0,   35,   36,   36,
	   36,   36,   36,   36,   36,   36,   36,    0,   92,    0,   36,   37,
	   36,   36,   36,   36,   36,   36,   36,   36,    0,   37,   38,   37,
	   41,   41,    0,   39,    0,   40,   35,   40,   36,   36,   36,   36,
	   36,   36,   36,   36,   36,    0,   40,   38,   40,   41,   41,    0,
	   42,   38,   42,   41,   41,    0,   42,   38,   42,   41,   41,    0,
	   44,    0,   45,    0,   46,    0,   47,    0,   48,    0,   49,    0,
	   50,    0,   51,    0,   52,    0,   53,    0,   54,    0,   55,    0,
	   56,    0,   56,   57,   56,   56,    0,   58,    0,   59,   60,   60,
	   60,   60,   60,   60,   60,   60,   60,    0,   93,    0,   60,   61,
	   60,   60,   60,   60,   60,   60,   60,   60,    0,   61,   62,   61,
	   65,   65,    0,   63,    0,   64,   59,   64,   60,   60,   60,   60,
	   60,   60,   60,   60,   60,    0,   64,   62,   64,   65,   65,    0,
	   66,   62,   66,   65,   65,    0,   66,   62,   66,   65,   65,    0,
	   68,   68,   68,   68,   68,   68,   68,   68,   68,    0,   69,   68,
	   68,   68,   68,   68,   68,   68,   68,   68,    0,    0,    0,   69,
	   70,   94,   75,   76,   70,    0,    0,   95,   71,   73,   98,   72,
	   73,   96,   73,    0,   73,   73,   96,  100,   74,    0,   94,   75,
	    0,   94,   75,   76,   77,    0,   78,    0,   79,    0,   80,    0,
	   81,    0,   82,    0,   83,    0,   84,    0,   94,   75,   84,    0,
	    0,   91,   13,    0,   86,   10,   86,   89,   11,   86,   87,   88,
	   89,   11,   88,   87,   88,   89,   11,   88,   87,   90,   91,   13,
	   90,   86,   10,   90,   89,   11,   90,   87,    0,    0,    0,    0,
	  100,   74,    0,   95,   71,   95,   98,   72,   95,   96,   97,   98,
	   72,   97,   96,   97,   98,   72,   97,   96,   99,  100,   74,   99,
	   95,   71,   99,   98,   72,   99,   96,    0,    0
	};
}

private static final byte _http_trans_targs[] = init__http_trans_targs_0();


private static byte[] init__http_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    7,    0,    7,    0,    7,    0,    0,    0,    0,    0,    0,    0,
	   21,    1,    1,    0,    1,    1,    1,    1,    0,    1,    1,    1,
	   17,    0,    0,    0,    0,    0,    1,   13,   13,    1,    1,    0,
	    3,    0,    0,    0,    0,    5,    0,    0,    0,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    0,   11,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    0,    1,    1,    1,    1,    1,    1,
	    1,    1,    0,   15,    1,    1,    1,    1,    1,    1,    1,    1,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    3,
	    0,    0,    0,    5,    0,    0,    0,    0,    0,    0,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,   17,    0,    1,   21,
	    1,    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,
	    1,    1,    0,    0,    0,    0,   23,    0,   31,   31,   31,   31,
	   31,   31,   31,   31,   31,    0,    0,   19,    0,   28,   28,    0,
	   25,    9,   25,    1,    1,    0,    1,    0,    1,    1,    1,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    3,    0,
	    0,    0,    5,    0,    0,    0,    7,    0,    7,    0,    7,    0,
	    0,    0,    1,   13,    1,    1,    0,    0,    0,    0,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,   17,    0,    1,   21,
	    1,    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,
	    1,    1,    0,    0,    0,    0,   23,    0,   31,   31,   31,   31,
	   31,   31,   31,   31,   31,    0,    0,   19,    0,   28,   28,    0,
	   25,    9,   25,    1,    1,    0,    1,    0,    1,    1,    1,    0,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    0,   11,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,
	    1,   15,   15,   15,    1,    0,    0,   21,    1,    1,    0,    1,
	    1,    1,    1,    0,    1,    1,    1,   17,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    3,    0,    0,    0,    5,    0,    0,    0,    0,    0,
	    0,   17,    0,    0,   21,    1,    0,    0,    1,    0,    1,   25,
	    9,   25,   25,    1,    1,    0,    1,    1,    1,    0,   34,   23,
	    0,   37,   31,    0,   19,   28,    0,   28,    0,    0,    0,    0,
	   17,    0,    0,   21,    1,    0,    0,    1,    0,    1,   25,    9,
	   25,   25,    1,    1,    0,    1,    1,    1,    0,   34,   23,    0,
	   37,   31,    0,   19,   28,    0,   28,    0,    0
	};
}

private static final byte _http_trans_actions[] = init__http_trans_actions_0();


private static byte[] init__http_eof_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,   17,   34,   40,   34,   34,   44,    0,    0,    0,   17,   34,
	   40,   34,   34,   44,    0
	};
}

private static final byte _http_eof_actions[] = init__http_eof_actions_0();


static final int http_start = 1;
static final int http_first_final = 85;
static final int http_error = 0;

static final int http_en_http_request = 20;
static final int http_en_http_response = 43;
static final int http_en_http_request_lenient = 67;
static final int http_en_http_response_lenient = 1;


// line 307 "HttpParser.rl"
}