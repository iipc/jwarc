
// line 1 "HttpParser.rl"
// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -

// line 112 "HttpParser.rl"


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

// line 149 "HttpParser.rl"
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
	{ bufPos = 0; }
	break;
	case 9:
// line 19 "HttpParser.rl"
	{ finished = true; { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
	case 10:
// line 21 "HttpParser.rl"
	{
    if (bufPos > 0) {
        bufPos = endOfText;
        push((byte)' ');
    }
}
	break;
	case 11:
// line 28 "HttpParser.rl"
	{
    name = new String(buf, 0, bufPos, US_ASCII).trim();
    bufPos = 0;
}
	break;
	case 12:
// line 33 "HttpParser.rl"
	{
    String value = new String(buf, 0, endOfText, ISO_8859_1);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}
	break;
// line 310 "HttpParser.java"
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
	case 9:
// line 19 "HttpParser.rl"
	{ finished = true; { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
	case 10:
// line 21 "HttpParser.rl"
	{
    if (bufPos > 0) {
        bufPos = endOfText;
        push((byte)' ');
    }
}
	break;
	case 12:
// line 33 "HttpParser.rl"
	{
    String value = new String(buf, 0, endOfText, ISO_8859_1);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}
	break;
// line 357 "HttpParser.java"
		}
	}
	}

case 5:
	}
	break; }
	}

// line 264 "HttpParser.rl"

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

    
// line 413 "HttpParser.java"
private static byte[] init__http_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   10,    1,
	   11,    1,   12,    2,    4,    0,    2,    8,    0,    2,    8,    9,
	    2,    8,   11,    2,   10,    0,    2,   12,    0,    2,   12,    8,
	    2,   12,    9,    2,   12,   11,    3,    4,   12,    9,    3,   10,
	   12,    9,    3,   12,    8,    0,    3,   12,    8,    9,    3,   12,
	    8,   11
	};
}

private static final byte _http_actions[] = init__http_actions_0();


private static short[] init__http_key_offsets_0()
{
	return new short [] {
	    0,    0,    1,    2,    3,    4,    6,    9,   11,   13,   16,   19,
	   24,   27,   29,   33,   37,   41,   46,   50,   54,   58,   60,   65,
	   70,   75,   80,   84,   88,   92,   96,   98,  105,  107,  109,  111,
	  112,  127,  143,  155,  168,  169,  170,  171,  172,  173,  175,  176,
	  178,  179,  180,  196,  197,  213,  220,  221,  239,  246,  253,  260,
	  261,  262,  263,  264,  265,  267,  268,  270,  271,  273,  275,  277,
	  278,  284,  285,  301,  302,  318,  325,  326,  344,  351,  358,  365,
	  380,  396,  399,  402,  405,  410,  413,  415,  419,  423,  427,  432,
	  436,  440,  444,  446,  451,  456,  461,  466,  470,  474,  478,  482,
	  484,  488,  489,  490,  491,  492,  494,  495,  497,  500,  505,  505,
	  509,  513,  517,  522,  526,  531,  536,  541,  546,  546,  546,  551,
	  551,  555,  559,  563,  568,  572,  577,  582,  587
	};
}

private static final short _http_key_offsets[] = init__http_key_offsets_0();


private static char[] init__http_trans_keys_0()
{
	return new char [] {
	   72,   84,   84,   80,   32,   47,   32,   48,   57,   48,   57,   48,
	   57,   10,   13,   32,   10,   13,   58,    9,   10,   13,   32,   58,
	   10,   13,   58,   10,   13,    9,   10,   13,   32,    9,   10,   13,
	   32,    9,   10,   13,   32,    9,   10,   13,   32,   58,    9,   10,
	   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,   10,   13,
	    9,   10,   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,
	   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,   13,   32,
	    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,
	   10,   13,    9,   10,   13,   32,  126,  128,  255,   48,   57,   32,
	   46,   48,   57,   32,   33,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,   32,   33,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   33,
	   61,   95,  126,   36,   59,   63,   90,   97,  122,  128,  255,   32,
	   33,   61,   95,  126,   36,   59,   63,   90,   97,  122,  128,  255,
	   72,   84,   84,   80,   47,   48,   57,   46,   48,   57,   13,   10,
	   13,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   10,   33,   58,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,
	   33,  126,  128,  255,   10,    9,   13,   32,   33,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   13,   32,   33,  126,  128,  255,    9,   13,   32,   33,  126,  128,
	  255,    9,   13,   32,   33,  126,  128,  255,   72,   84,   84,   80,
	   47,   48,   57,   46,   48,   57,   32,   48,   57,   48,   57,   48,
	   57,   32,    9,   13,   32,  126,  128,  255,   10,   13,   33,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,   10,   33,   58,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,    9,   13,   32,   33,  126,  128,
	  255,   10,    9,   13,   32,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,   33,
	  126,  128,  255,    9,   13,   32,   33,  126,  128,  255,    9,   13,
	   32,   33,  126,  128,  255,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   32,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	   10,   13,   32,   10,   13,   32,   10,   13,   58,    9,   10,   13,
	   32,   58,   10,   13,   58,   10,   13,    9,   10,   13,   32,    9,
	   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,   58,
	    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,
	   10,   13,    9,   10,   13,   32,   58,    9,   10,   13,   32,   58,
	    9,   10,   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,
	   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,
	   13,   32,   10,   13,   10,   13,   32,   72,   84,   84,   80,   47,
	   48,   57,   46,   48,   57,   10,   13,   32,    9,   10,   13,   32,
	   58,    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,
	   32,    9,   10,   13,   32,   58,    9,   10,   13,   32,    9,   10,
	   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,   13,   32,
	   58,    9,   10,   13,   32,   58,    9,   10,   13,   32,   58,    9,
	   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,    9,
	   10,   13,   32,   58,    9,   10,   13,   32,    9,   10,   13,   32,
	   58,    9,   10,   13,   32,   58,    9,   10,   13,   32,   58,    9,
	   10,   13,   32,   58,    0
	};
}

private static final char _http_trans_keys[] = init__http_trans_keys_0();


private static byte[] init__http_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    2,    1,    0,    0,    3,    3,    5,
	    3,    2,    4,    4,    4,    5,    4,    4,    4,    2,    5,    5,
	    5,    5,    4,    4,    4,    4,    2,    3,    0,    2,    0,    1,
	    3,    4,    4,    5,    1,    1,    1,    1,    1,    0,    1,    0,
	    1,    1,    4,    1,    4,    3,    1,    6,    3,    3,    3,    1,
	    1,    1,    1,    1,    0,    1,    0,    1,    0,    0,    0,    1,
	    2,    1,    4,    1,    4,    3,    1,    6,    3,    3,    3,    3,
	    4,    3,    3,    3,    5,    3,    2,    4,    4,    4,    5,    4,
	    4,    4,    2,    5,    5,    5,    5,    4,    4,    4,    4,    2,
	    4,    1,    1,    1,    1,    0,    1,    0,    3,    5,    0,    4,
	    4,    4,    5,    4,    5,    5,    5,    5,    0,    0,    5,    0,
	    4,    4,    4,    5,    4,    5,    5,    5,    5
	};
}

private static final byte _http_single_lengths[] = init__http_single_lengths_0();


private static byte[] init__http_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    1,    1,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    2,    1,    0,    1,    0,
	    6,    6,    4,    4,    0,    0,    0,    0,    0,    1,    0,    1,
	    0,    0,    6,    0,    6,    2,    0,    6,    2,    2,    2,    0,
	    0,    0,    0,    0,    1,    0,    1,    0,    1,    1,    1,    0,
	    2,    0,    6,    0,    6,    2,    0,    6,    2,    2,    2,    6,
	    6,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    1,    0,    1,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0
	};
}

private static final byte _http_range_lengths[] = init__http_range_lengths_0();


private static short[] init__http_index_offsets_0()
{
	return new short [] {
	    0,    0,    2,    4,    6,    8,   11,   14,   16,   18,   22,   26,
	   32,   36,   39,   44,   49,   54,   60,   65,   70,   75,   78,   84,
	   90,   96,  102,  107,  112,  117,  122,  125,  131,  133,  136,  138,
	  140,  150,  161,  170,  180,  182,  184,  186,  188,  190,  192,  194,
	  196,  198,  200,  211,  213,  224,  230,  232,  245,  251,  257,  263,
	  265,  267,  269,  271,  273,  275,  277,  279,  281,  283,  285,  287,
	  289,  294,  296,  307,  309,  320,  326,  328,  341,  347,  353,  359,
	  369,  380,  384,  388,  392,  398,  402,  405,  410,  415,  420,  426,
	  431,  436,  441,  444,  450,  456,  462,  468,  473,  478,  483,  488,
	  491,  496,  498,  500,  502,  504,  506,  508,  510,  514,  520,  521,
	  526,  531,  536,  542,  547,  553,  559,  565,  571,  572,  573,  579,
	  580,  585,  590,  595,  601,  606,  612,  618,  624
	};
}

private static final short _http_index_offsets[] = init__http_index_offsets_0();


private static short[] init__http_trans_targs_0()
{
	return new short [] {
	    2,    0,    3,    0,    4,    0,    5,    0,    6,   32,    0,    6,
	    7,    0,    8,    0,    9,    0,  117,   30,   31,    0,   11,   13,
	  119,   10,    0,  118,   21,    0,   22,   12,   11,   13,   14,   12,
	   11,   13,    0,   14,   17,   19,   14,   15,   16,   17,   19,   16,
	   15,   16,   17,   19,   16,   15,   18,  118,   21,   18,   22,   12,
	   18,   17,   19,   18,   15,   20,   17,   19,   20,   15,   20,    0,
	   20,   20,   15,  118,   21,    0,   22,   25,   26,   22,   15,   23,
	   24,   25,   26,   24,   15,   23,   24,   25,   26,   24,   15,   23,
	   18,  118,   21,   18,   22,   12,   20,   25,   26,   20,   15,   28,
	  122,   27,   28,  120,   28,    0,   28,   28,  120,   28,  127,   29,
	   28,  120,  117,   30,    0,   31,  117,   30,   31,   31,    0,   33,
	    0,    6,   34,    0,   35,    0,    6,    0,   37,   37,   37,   37,
	   37,   37,   37,   37,   37,    0,   38,   37,   37,   37,   37,   37,
	   37,   37,   37,   37,    0,   39,   39,   39,   39,   39,   39,   39,
	   39,    0,   40,   39,   39,   39,   39,   39,   39,   39,   39,    0,
	   41,    0,   42,    0,   43,    0,   44,    0,   45,    0,   46,    0,
	   47,    0,   48,    0,   49,    0,   50,    0,   51,   52,   52,   52,
	   52,   52,   52,   52,   52,   52,    0,  128,    0,   52,   53,   52,
	   52,   52,   52,   52,   52,   52,   52,    0,   53,   54,   53,   57,
	   57,    0,   55,    0,   56,   51,   56,   52,   52,   52,   52,   52,
	   52,   52,   52,   52,    0,   56,   54,   56,   57,   57,    0,   58,
	   54,   58,   57,   57,    0,   58,   54,   58,   57,   57,    0,   60,
	    0,   61,    0,   62,    0,   63,    0,   64,    0,   65,    0,   66,
	    0,   67,    0,   68,    0,   69,    0,   70,    0,   71,    0,   72,
	    0,   72,   73,   72,   72,    0,   74,    0,   75,   76,   76,   76,
	   76,   76,   76,   76,   76,   76,    0,  129,    0,   76,   77,   76,
	   76,   76,   76,   76,   76,   76,   76,    0,   77,   78,   77,   81,
	   81,    0,   79,    0,   80,   75,   80,   76,   76,   76,   76,   76,
	   76,   76,   76,   76,    0,   80,   78,   80,   81,   81,    0,   82,
	   78,   82,   81,   81,    0,   82,   78,   82,   81,   81,    0,   84,
	   84,   84,   84,   84,   84,   84,   84,   84,    0,   85,   84,   84,
	   84,   84,   84,   84,   84,   84,   84,    0,    0,    0,   85,   86,
	  130,  107,  108,   86,   88,   90,  132,   87,    0,  131,   98,    0,
	   99,   89,   88,   90,   91,   89,   88,   90,    0,   91,   94,   96,
	   91,   92,   93,   94,   96,   93,   92,   93,   94,   96,   93,   92,
	   95,  131,   98,   95,   99,   89,   95,   94,   96,   95,   92,   97,
	   94,   96,   97,   92,   97,    0,   97,   97,   92,  131,   98,    0,
	   99,  102,  103,   99,   92,  100,  101,  102,  103,  101,   92,  100,
	  101,  102,  103,  101,   92,  100,   95,  131,   98,   95,   99,   89,
	   97,  102,  103,   97,   92,  105,  135,  104,  105,  133,  105,    0,
	  105,  105,  133,  105,  140,  106,  105,  133,  130,  107,    0,  130,
	  107,  108,  109,    0,  110,    0,  111,    0,  112,    0,  113,    0,
	  114,    0,  115,    0,  116,    0,  130,  107,  116,    0,    0,  118,
	   21,    0,  124,   10,    0,  119,  122,   27,  119,  120,  121,  122,
	   27,  121,  120,  121,  122,   27,  121,  120,  123,  118,   21,  123,
	  124,   10,  123,  122,   27,  123,  120,  124,  127,   29,  124,  120,
	  125,  126,  127,   29,  126,  120,  125,  126,  127,   29,  126,  120,
	  125,  123,  118,   21,  123,  124,   10,    0,    0,    0,  131,   98,
	    0,  137,   87,    0,  132,  135,  104,  132,  133,  134,  135,  104,
	  134,  133,  134,  135,  104,  134,  133,  136,  131,   98,  136,  137,
	   87,  136,  135,  104,  136,  133,  137,  140,  106,  137,  133,  138,
	  139,  140,  106,  139,  133,  138,  139,  140,  106,  139,  133,  138,
	  136,  131,   98,  136,  137,   87,    0
	};
}

private static final short _http_trans_targs[] = init__http_trans_targs_0();


private static byte[] init__http_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    7,    0,    7,    0,    7,    0,    0,    0,    0,    0,    0,    0,
	   23,    1,    0,   33,   17,    0,   36,   30,    0,    0,   23,    1,
	    0,    0,    0,    0,    0,    1,    0,    1,   27,    9,   27,   27,
	    1,    1,    0,    1,    1,    1,    0,   48,   25,    0,   51,   42,
	    0,   21,   39,    0,   39,    1,    0,    1,    1,    1,    1,    0,
	    1,    1,    1,   19,    0,    0,    0,    0,    1,    0,    1,    1,
	   27,    9,   27,   27,    1,    1,    1,    0,    1,    1,    1,    1,
	    0,   66,   45,    0,   70,   62,    1,    0,    1,    1,    1,    1,
	    0,    1,    1,    1,    1,    0,    1,    1,    1,    1,    0,    1,
	    1,    1,    0,    0,    0,    1,   13,   13,    1,    1,    0,    3,
	    0,    0,    0,    0,    5,    0,    0,    0,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    0,   11,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    0,    1,    1,    1,    1,    1,    1,    1,
	    1,    0,   15,    1,    1,    1,    1,    1,    1,    1,    1,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    3,    0,
	    0,    0,    5,    0,    0,    0,    0,    0,    0,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    0,   19,    0,    1,   23,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,    1,
	    1,    0,    0,    0,    0,   25,    0,   42,   42,   42,   42,   42,
	   42,   42,   42,   42,    0,    0,   21,    0,   39,   39,    0,   27,
	    9,   27,    1,    1,    0,    1,    0,    1,    1,    1,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    3,    0,    0,
	    0,    5,    0,    0,    0,    7,    0,    7,    0,    7,    0,    0,
	    0,    1,   13,    1,    1,    0,    0,    0,    0,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    0,   19,    0,    1,   23,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,    1,
	    1,    0,    0,    0,    0,   25,    0,   42,   42,   42,   42,   42,
	   42,   42,   42,   42,    0,    0,   21,    0,   39,   39,    0,   27,
	    9,   27,    1,    1,    0,    1,    0,    1,    1,    1,    0,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    0,   11,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,    1,
	   15,   15,   15,    1,    0,    0,   23,    1,    0,   33,   17,    0,
	   36,   30,    0,    0,   23,    1,    0,    0,    0,    0,    0,    1,
	    0,    1,   27,    9,   27,   27,    1,    1,    0,    1,    1,    1,
	    0,   48,   25,    0,   51,   42,    0,   21,   39,    0,   39,    1,
	    0,    1,    1,    1,    1,    0,    1,    1,    1,   19,    0,    0,
	    0,    0,    1,    0,    1,    1,   27,    9,   27,   27,    1,    1,
	    1,    0,    1,    1,    1,    1,    0,   66,   45,    0,   70,   62,
	    1,    0,    1,    1,    1,    1,    0,    1,    1,    1,    1,    0,
	    1,    1,    1,    1,    0,    1,    1,    1,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    3,    0,    0,    0,    5,    0,    0,    0,    0,    0,    0,   19,
	    0,    0,   23,    1,    0,    0,    0,    1,    0,    1,   27,    9,
	   27,   27,    1,    1,    0,    1,    1,    1,    0,   48,   25,    0,
	   51,   42,    0,   21,   39,    0,   39,    0,    0,    1,    0,    1,
	    1,   27,    9,   27,   27,    1,    1,    1,    0,    1,    1,    1,
	    1,    0,   66,   45,    0,   70,   62,    0,    0,    0,   19,    0,
	    0,   23,    1,    0,    0,    0,    1,    0,    1,   27,    9,   27,
	   27,    1,    1,    0,    1,    1,    1,    0,   48,   25,    0,   51,
	   42,    0,   21,   39,    0,   39,    0,    0,    1,    0,    1,    1,
	   27,    9,   27,   27,    1,    1,    1,    0,    1,    1,    1,    1,
	    0,   66,   45,    0,   70,   62,    0
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
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,   19,    0,   48,
	   54,   48,   48,   58,   48,   54,   48,   48,    0,    0,   19,    0,
	   48,   54,   48,   48,   58,   48,   54,   48,   48
	};
}

private static final byte _http_eof_actions[] = init__http_eof_actions_0();


static final int http_start = 1;
static final int http_first_final = 117;
static final int http_error = 0;

static final int http_en_http_request = 36;
static final int http_en_http_response = 59;
static final int http_en_http_request_lenient = 83;
static final int http_en_http_response_lenient = 1;


// line 309 "HttpParser.rl"
}