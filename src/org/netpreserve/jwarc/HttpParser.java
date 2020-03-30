
// line 1 "HttpParser.rl"
// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -

// line 102 "HttpParser.rl"


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

// line 139 "HttpParser.rl"
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

// line 240 "HttpParser.rl"

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
	    8
	};
}

private static final byte _http_actions[] = init__http_actions_0();


private static short[] init__http_key_offsets_0()
{
	return new short [] {
	    0,    0,    1,    2,    3,    4,    5,    7,    8,   10,   11,   14,
	   16,   18,   21,   38,   39,   57,   60,   64,   68,   72,   89,   93,
	   97,  116,  120,  121,  128,  143,  159,  171,  184,  185,  186,  187,
	  188,  189,  191,  192,  194,  195,  196,  212,  213,  229,  236,  237,
	  255,  262,  269,  276,  277,  278,  279,  280,  281,  283,  284,  286,
	  287,  289,  291,  293,  294,  300,  301,  317,  318,  334,  341,  342,
	  360,  367,  374,  381,  396,  412,  414,  416,  418,  419,  420,  421,
	  422,  424,  425,  427,  430,  447,  448,  466,  469,  473,  477,  481,
	  498,  502,  506,  525,  529,  530,  530,  530,  530
	};
}

private static final short _http_key_offsets[] = init__http_key_offsets_0();


private static char[] init__http_trans_keys_0()
{
	return new char [] {
	   72,   84,   84,   80,   47,   48,   57,   46,   48,   57,   32,   32,
	   48,   57,   48,   57,   48,   57,   10,   13,   32,   10,   13,   33,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   10,    9,   32,   33,   58,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   32,   58,
	    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,
	   10,   13,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,    9,   10,   13,   32,    9,   10,   13,
	   32,    9,   10,   13,   32,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,    9,   10,   13,   32,
	   10,    9,   10,   13,   32,  126,  128,  255,   33,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   32,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   33,   61,   95,  126,   36,   59,   63,   90,   97,
	  122,  128,  255,   32,   33,   61,   95,  126,   36,   59,   63,   90,
	   97,  122,  128,  255,   72,   84,   84,   80,   47,   48,   57,   46,
	   48,   57,   13,   10,   13,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   10,   33,   58,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   13,   32,   33,  126,  128,  255,   10,    9,   13,   32,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   13,   32,   33,  126,  128,  255,    9,   13,
	   32,   33,  126,  128,  255,    9,   13,   32,   33,  126,  128,  255,
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
	   65,   90,   94,  122,   10,   32,   10,   32,   32,   72,   84,   84,
	   80,   47,   48,   57,   46,   48,   57,   10,   13,   32,   10,   13,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   10,    9,   32,   33,   58,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   32,
	   58,    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,
	   32,   10,   13,   33,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,    9,   10,   13,   32,    9,   10,
	   13,   32,    9,   10,   13,   32,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   10,   13,
	   32,   10,    0
	};
}

private static final char _http_trans_keys[] = init__http_trans_keys_0();


private static byte[] init__http_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    0,
	    0,    3,    5,    1,    6,    3,    4,    4,    4,    5,    4,    4,
	    7,    4,    1,    3,    3,    4,    4,    5,    1,    1,    1,    1,
	    1,    0,    1,    0,    1,    1,    4,    1,    4,    3,    1,    6,
	    3,    3,    3,    1,    1,    1,    1,    1,    0,    1,    0,    1,
	    0,    0,    0,    1,    2,    1,    4,    1,    4,    3,    1,    6,
	    3,    3,    3,    3,    4,    2,    2,    2,    1,    1,    1,    1,
	    0,    1,    0,    3,    5,    1,    6,    3,    4,    4,    4,    5,
	    4,    4,    7,    4,    1,    0,    0,    0,    0
	};
}

private static final byte _http_single_lengths[] = init__http_single_lengths_0();


private static byte[] init__http_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    0,    1,    0,    1,    1,
	    1,    0,    6,    0,    6,    0,    0,    0,    0,    6,    0,    0,
	    6,    0,    0,    2,    6,    6,    4,    4,    0,    0,    0,    0,
	    0,    1,    0,    1,    0,    0,    6,    0,    6,    2,    0,    6,
	    2,    2,    2,    0,    0,    0,    0,    0,    1,    0,    1,    0,
	    1,    1,    1,    0,    2,    0,    6,    0,    6,    2,    0,    6,
	    2,    2,    2,    6,    6,    0,    0,    0,    0,    0,    0,    0,
	    1,    0,    1,    0,    6,    0,    6,    0,    0,    0,    0,    6,
	    0,    0,    6,    0,    0,    0,    0,    0,    0
	};
}

private static final byte _http_range_lengths[] = init__http_range_lengths_0();


private static short[] init__http_index_offsets_0()
{
	return new short [] {
	    0,    0,    2,    4,    6,    8,   10,   12,   14,   16,   18,   21,
	   23,   25,   29,   41,   43,   56,   60,   65,   70,   75,   87,   92,
	   97,  111,  116,  118,  124,  134,  145,  154,  164,  166,  168,  170,
	  172,  174,  176,  178,  180,  182,  184,  195,  197,  208,  214,  216,
	  229,  235,  241,  247,  249,  251,  253,  255,  257,  259,  261,  263,
	  265,  267,  269,  271,  273,  278,  280,  291,  293,  304,  310,  312,
	  325,  331,  337,  343,  353,  364,  367,  370,  373,  375,  377,  379,
	  381,  383,  385,  387,  391,  403,  405,  418,  422,  427,  432,  437,
	  449,  454,  459,  473,  478,  480,  481,  482,  483
	};
}

private static final short _http_index_offsets[] = init__http_index_offsets_0();


private static byte[] init__http_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    0,    4,    0,    5,    0,    6,    0,    7,    0,
	    8,    0,    9,    0,   10,    0,   10,   11,    0,   12,    0,   13,
	    0,   14,   26,   27,    0,  101,   15,   16,   16,   16,   16,   16,
	   16,   16,   16,   16,    0,  101,    0,   17,   17,   16,   18,   16,
	   16,   16,   16,   16,   16,   16,   16,    0,   17,   17,   18,    0,
	   18,   21,   22,   18,   19,   20,   21,   22,   20,   19,   20,   21,
	   22,   20,   19,  101,   15,   16,   16,   16,   16,   16,   16,   16,
	   16,   16,    0,   23,   24,   23,   23,   19,   23,    0,   23,   23,
	   19,   25,  101,   15,   25,   16,   16,   16,   16,   16,   16,   16,
	   16,   16,    0,   25,   21,   22,   25,   19,   14,    0,   27,   14,
	   26,   27,   27,    0,   29,   29,   29,   29,   29,   29,   29,   29,
	   29,    0,   30,   29,   29,   29,   29,   29,   29,   29,   29,   29,
	    0,   31,   31,   31,   31,   31,   31,   31,   31,    0,   32,   31,
	   31,   31,   31,   31,   31,   31,   31,    0,   33,    0,   34,    0,
	   35,    0,   36,    0,   37,    0,   38,    0,   39,    0,   40,    0,
	   41,    0,   42,    0,   43,   44,   44,   44,   44,   44,   44,   44,
	   44,   44,    0,  102,    0,   44,   45,   44,   44,   44,   44,   44,
	   44,   44,   44,    0,   45,   46,   45,   49,   49,    0,   47,    0,
	   48,   43,   48,   44,   44,   44,   44,   44,   44,   44,   44,   44,
	    0,   48,   46,   48,   49,   49,    0,   50,   46,   50,   49,   49,
	    0,   50,   46,   50,   49,   49,    0,   52,    0,   53,    0,   54,
	    0,   55,    0,   56,    0,   57,    0,   58,    0,   59,    0,   60,
	    0,   61,    0,   62,    0,   63,    0,   64,    0,   64,   65,   64,
	   64,    0,   66,    0,   67,   68,   68,   68,   68,   68,   68,   68,
	   68,   68,    0,  103,    0,   68,   69,   68,   68,   68,   68,   68,
	   68,   68,   68,    0,   69,   70,   69,   73,   73,    0,   71,    0,
	   72,   67,   72,   68,   68,   68,   68,   68,   68,   68,   68,   68,
	    0,   72,   70,   72,   73,   73,    0,   74,   70,   74,   73,   73,
	    0,   74,   70,   74,   73,   73,    0,   76,   76,   76,   76,   76,
	   76,   76,   76,   76,    0,   77,   76,   76,   76,   76,   76,   76,
	   76,   76,   76,    0,    0,   77,   78,    0,   79,   78,   79,   80,
	    0,   81,    0,   82,    0,   83,    0,   84,    0,   85,    0,   86,
	    0,   87,    0,   88,  100,   87,    0,  104,   89,   90,   90,   90,
	   90,   90,   90,   90,   90,   90,    0,  104,    0,   91,   91,   90,
	   92,   90,   90,   90,   90,   90,   90,   90,   90,    0,   91,   91,
	   92,    0,   92,   95,   96,   92,   93,   94,   95,   96,   94,   93,
	   94,   95,   96,   94,   93,  104,   89,   90,   90,   90,   90,   90,
	   90,   90,   90,   90,    0,   97,   98,   97,   97,   93,   97,    0,
	   97,   97,   93,   99,  104,   89,   99,   90,   90,   90,   90,   90,
	   90,   90,   90,   90,    0,   99,   95,   96,   99,   93,   88,    0,
	    0,    0,    0,    0,    0
	};
}

private static final byte _http_trans_targs[] = init__http_trans_targs_0();


private static byte[] init__http_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    3,    0,
	    0,    0,    5,    0,    0,    0,    0,    7,    0,    7,    0,    7,
	    0,    0,    0,    0,    0,   17,    0,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    0,   17,    0,   21,   21,    1,   21,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,    0,
	    0,    0,    1,    0,    1,   25,    9,   25,   25,    1,    1,    0,
	    1,    1,    1,   34,   23,   31,   31,   31,   31,   31,   31,   31,
	   31,   31,    0,    1,    0,    1,    1,    1,    1,    0,    1,    1,
	    1,    0,   34,   23,    0,   31,   31,   31,   31,   31,   31,   31,
	   31,   31,    0,    0,   19,   28,    0,   28,    0,    0,    1,   13,
	   13,    1,    1,    0,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    0,   11,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    0,    1,    1,    1,    1,    1,    1,    1,    1,    0,   15,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    3,    0,    0,    0,    5,    0,
	    0,    0,    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    0,   17,    0,    1,   21,    1,    1,    1,    1,    1,
	    1,    1,    1,    0,    0,    0,    0,    1,    1,    0,    0,    0,
	    0,   23,    0,   31,   31,   31,   31,   31,   31,   31,   31,   31,
	    0,    0,   19,    0,   28,   28,    0,   25,    9,   25,    1,    1,
	    0,    1,    0,    1,    1,    1,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    3,    0,    0,    0,    5,    0,    0,
	    0,    7,    0,    7,    0,    7,    0,    0,    0,    1,   13,    1,
	    1,    0,    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    0,   17,    0,    1,   21,    1,    1,    1,    1,    1,
	    1,    1,    1,    0,    0,    0,    0,    1,    1,    0,    0,    0,
	    0,   23,    0,   31,   31,   31,   31,   31,   31,   31,   31,   31,
	    0,    0,   19,    0,   28,   28,    0,   25,    9,   25,    1,    1,
	    0,    1,    0,    1,    1,    1,    0,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    0,   11,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    0,    0,    0,    1,    0,   15,    1,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    3,    0,    0,
	    0,    5,    0,    0,    0,    0,    0,   17,    0,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    0,   17,    0,   21,   21,    1,
	   21,    1,    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,
	    0,    0,    0,    0,    1,    0,    1,   25,    9,   25,   25,    1,
	    1,    0,    1,    1,    1,   34,   23,   31,   31,   31,   31,   31,
	   31,   31,   31,   31,    0,    1,    0,    1,    1,    1,    1,    0,
	    1,    1,    1,    0,   34,   23,    0,   31,   31,   31,   31,   31,
	   31,   31,   31,   31,    0,    0,   19,   28,    0,   28,    0,    0,
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


// line 280 "HttpParser.rl"
}