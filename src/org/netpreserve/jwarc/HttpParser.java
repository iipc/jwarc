
// line 1 "HttpParser.rl"
// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -

// line 113 "HttpParser.rl"


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

// line 150 "HttpParser.rl"
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

// line 265 "HttpParser.rl"

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
	   70,   75,   80,   84,   88,   92,   96,  100,  104,  108,  112,  114,
	  121,  123,  125,  127,  128,  143,  159,  171,  184,  185,  186,  187,
	  188,  189,  191,  192,  194,  195,  196,  212,  213,  229,  236,  237,
	  255,  262,  269,  276,  277,  278,  279,  280,  281,  283,  284,  286,
	  287,  289,  291,  293,  294,  300,  301,  317,  318,  334,  341,  342,
	  360,  367,  374,  381,  396,  412,  415,  418,  421,  426,  429,  431,
	  435,  439,  443,  448,  452,  456,  460,  462,  467,  472,  477,  482,
	  486,  490,  494,  498,  500,  504,  505,  506,  507,  508,  510,  511,
	  513,  516,  521,  521,  525,  529,  533,  538,  542,  547,  552,  557,
	  562,  567,  567,  567,  572,  572,  576,  580,  584,  589,  593,  598,
	  603,  608
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
	    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,
	    9,   10,   13,   32,   10,   13,    9,   10,   13,   32,  126,  128,
	  255,   48,   57,   32,   46,   48,   57,   32,   33,  124,  126,   35,
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
	   65,   90,   94,  122,   10,   13,   32,   10,   13,   32,   10,   13,
	   58,    9,   10,   13,   32,   58,   10,   13,   58,   10,   13,    9,
	   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,    9,
	   10,   13,   32,   58,    9,   10,   13,   32,    9,   10,   13,   32,
	    9,   10,   13,   32,   10,   13,    9,   10,   13,   32,   58,    9,
	   10,   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,   13,
	   32,   58,    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,
	   13,   32,    9,   10,   13,   32,   10,   13,   10,   13,   32,   72,
	   84,   84,   80,   47,   48,   57,   46,   48,   57,   10,   13,   32,
	    9,   10,   13,   32,   58,    9,   10,   13,   32,    9,   10,   13,
	   32,    9,   10,   13,   32,    9,   10,   13,   32,   58,    9,   10,
	   13,   32,    9,   10,   13,   32,   58,    9,   10,   13,   32,   58,
	    9,   10,   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,
	   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,   13,   32,
	    9,   10,   13,   32,    9,   10,   13,   32,    9,   10,   13,   32,
	   58,    9,   10,   13,   32,    9,   10,   13,   32,   58,    9,   10,
	   13,   32,   58,    9,   10,   13,   32,   58,    9,   10,   13,   32,
	   58,    0
	};
}

private static final char _http_trans_keys[] = init__http_trans_keys_0();


private static byte[] init__http_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    2,    1,    0,    0,    3,    3,    5,
	    3,    2,    4,    4,    4,    5,    4,    4,    4,    2,    5,    5,
	    5,    5,    4,    4,    4,    4,    4,    4,    4,    4,    2,    3,
	    0,    2,    0,    1,    3,    4,    4,    5,    1,    1,    1,    1,
	    1,    0,    1,    0,    1,    1,    4,    1,    4,    3,    1,    6,
	    3,    3,    3,    1,    1,    1,    1,    1,    0,    1,    0,    1,
	    0,    0,    0,    1,    2,    1,    4,    1,    4,    3,    1,    6,
	    3,    3,    3,    3,    4,    3,    3,    3,    5,    3,    2,    4,
	    4,    4,    5,    4,    4,    4,    2,    5,    5,    5,    5,    4,
	    4,    4,    4,    2,    4,    1,    1,    1,    1,    0,    1,    0,
	    3,    5,    0,    4,    4,    4,    5,    4,    5,    5,    5,    5,
	    5,    0,    0,    5,    0,    4,    4,    4,    5,    4,    5,    5,
	    5,    5
	};
}

private static final byte _http_single_lengths[] = init__http_single_lengths_0();


private static byte[] init__http_range_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    0,    0,    0,    1,    1,    1,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    2,
	    1,    0,    1,    0,    6,    6,    4,    4,    0,    0,    0,    0,
	    0,    1,    0,    1,    0,    0,    6,    0,    6,    2,    0,    6,
	    2,    2,    2,    0,    0,    0,    0,    0,    1,    0,    1,    0,
	    1,    1,    1,    0,    2,    0,    6,    0,    6,    2,    0,    6,
	    2,    2,    2,    6,    6,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    1,    0,    1,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0
	};
}

private static final byte _http_range_lengths[] = init__http_range_lengths_0();


private static short[] init__http_index_offsets_0()
{
	return new short [] {
	    0,    0,    2,    4,    6,    8,   11,   14,   16,   18,   22,   26,
	   32,   36,   39,   44,   49,   54,   60,   65,   70,   75,   78,   84,
	   90,   96,  102,  107,  112,  117,  122,  127,  132,  137,  142,  145,
	  151,  153,  156,  158,  160,  170,  181,  190,  200,  202,  204,  206,
	  208,  210,  212,  214,  216,  218,  220,  231,  233,  244,  250,  252,
	  265,  271,  277,  283,  285,  287,  289,  291,  293,  295,  297,  299,
	  301,  303,  305,  307,  309,  314,  316,  327,  329,  340,  346,  348,
	  361,  367,  373,  379,  389,  400,  404,  408,  412,  418,  422,  425,
	  430,  435,  440,  446,  451,  456,  461,  464,  470,  476,  482,  488,
	  493,  498,  503,  508,  511,  516,  518,  520,  522,  524,  526,  528,
	  530,  534,  540,  541,  546,  551,  556,  562,  567,  573,  579,  585,
	  591,  597,  598,  599,  605,  606,  611,  616,  621,  627,  632,  638,
	  644,  650
	};
}

private static final short _http_index_offsets[] = init__http_index_offsets_0();


private static short[] init__http_trans_targs_0()
{
	return new short [] {
	    2,    0,    3,    0,    4,    0,    5,    0,    6,   36,    0,    6,
	    7,    0,    8,    0,    9,    0,  121,   34,   35,    0,   11,   13,
	  123,   10,    0,  122,   21,    0,   22,   12,   11,   13,   14,   12,
	   11,   13,    0,   14,   17,   19,   14,   15,   16,   17,   19,   16,
	   15,   16,   17,   19,   16,   15,   18,  122,   21,   18,   22,   12,
	   18,   17,   19,   18,   15,   20,   17,   19,   20,   15,   20,    0,
	   20,   20,   15,  122,   21,    0,   22,   25,   26,   22,   15,   23,
	   24,   25,   26,   24,   15,   23,   24,   25,   26,   24,   15,   23,
	   18,  122,   21,   18,   22,   12,   20,   25,   26,   20,   15,   28,
	  126,   27,   28,  124,   28,    0,   28,   28,  124,   28,  131,   29,
	   28,  124,   30,    0,   30,   30,   31,   32,  132,   33,   32,   31,
	   32,  132,   33,   32,   31,   30,  132,   33,   30,   31,  121,   34,
	    0,   35,  121,   34,   35,   35,    0,   37,    0,    6,   38,    0,
	   39,    0,    6,    0,   41,   41,   41,   41,   41,   41,   41,   41,
	   41,    0,   42,   41,   41,   41,   41,   41,   41,   41,   41,   41,
	    0,   43,   43,   43,   43,   43,   43,   43,   43,    0,   44,   43,
	   43,   43,   43,   43,   43,   43,   43,    0,   45,    0,   46,    0,
	   47,    0,   48,    0,   49,    0,   50,    0,   51,    0,   52,    0,
	   53,    0,   54,    0,   55,   56,   56,   56,   56,   56,   56,   56,
	   56,   56,    0,  133,    0,   56,   57,   56,   56,   56,   56,   56,
	   56,   56,   56,    0,   57,   58,   57,   61,   61,    0,   59,    0,
	   60,   55,   60,   56,   56,   56,   56,   56,   56,   56,   56,   56,
	    0,   60,   58,   60,   61,   61,    0,   62,   58,   62,   61,   61,
	    0,   62,   58,   62,   61,   61,    0,   64,    0,   65,    0,   66,
	    0,   67,    0,   68,    0,   69,    0,   70,    0,   71,    0,   72,
	    0,   73,    0,   74,    0,   75,    0,   76,    0,   76,   77,   76,
	   76,    0,   78,    0,   79,   80,   80,   80,   80,   80,   80,   80,
	   80,   80,    0,  134,    0,   80,   81,   80,   80,   80,   80,   80,
	   80,   80,   80,    0,   81,   82,   81,   85,   85,    0,   83,    0,
	   84,   79,   84,   80,   80,   80,   80,   80,   80,   80,   80,   80,
	    0,   84,   82,   84,   85,   85,    0,   86,   82,   86,   85,   85,
	    0,   86,   82,   86,   85,   85,    0,   88,   88,   88,   88,   88,
	   88,   88,   88,   88,    0,   89,   88,   88,   88,   88,   88,   88,
	   88,   88,   88,    0,    0,    0,   89,   90,  135,  111,  112,   90,
	   92,   94,  137,   91,    0,  136,  102,    0,  103,   93,   92,   94,
	   95,   93,   92,   94,    0,   95,   98,  100,   95,   96,   97,   98,
	  100,   97,   96,   97,   98,  100,   97,   96,   99,  136,  102,   99,
	  103,   93,   99,   98,  100,   99,   96,  101,   98,  100,  101,   96,
	  101,    0,  101,  101,   96,  136,  102,    0,  103,  106,  107,  103,
	   96,  104,  105,  106,  107,  105,   96,  104,  105,  106,  107,  105,
	   96,  104,   99,  136,  102,   99,  103,   93,  101,  106,  107,  101,
	   96,  109,  140,  108,  109,  138,  109,    0,  109,  109,  138,  109,
	  145,  110,  109,  138,  135,  111,    0,  135,  111,  112,  113,    0,
	  114,    0,  115,    0,  116,    0,  117,    0,  118,    0,  119,    0,
	  120,    0,  135,  111,  120,    0,   30,  122,   21,   30,  128,   10,
	    0,  123,  126,   27,  123,  124,  125,  126,   27,  125,  124,  125,
	  126,   27,  125,  124,  127,  122,   21,  127,  128,   10,  127,  126,
	   27,  127,  124,  128,  131,   29,  128,  124,  129,  130,  131,   29,
	  130,  124,  129,  130,  131,   29,  130,  124,  129,  127,  122,   21,
	  127,  128,   10,    0,  122,   21,    0,  128,   10,    0,    0,    0,
	  136,  102,    0,  142,   91,    0,  137,  140,  108,  137,  138,  139,
	  140,  108,  139,  138,  139,  140,  108,  139,  138,  141,  136,  102,
	  141,  142,   91,  141,  140,  108,  141,  138,  142,  145,  110,  142,
	  138,  143,  144,  145,  110,  144,  138,  143,  144,  145,  110,  144,
	  138,  143,  141,  136,  102,  141,  142,   91,    0
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
	    1,    1,    0,    0,    0,    0,    0,    9,    9,    9,    9,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    1,   13,   13,    1,    1,    0,    3,    0,    0,    0,    0,
	    5,    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    0,   11,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    0,    1,    1,    1,    1,    1,    1,    1,    1,    0,   15,    1,
	    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    3,    0,    0,    0,    5,    0,
	    0,    0,    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    0,   19,    0,    1,   23,    1,    1,    1,    1,    1,
	    1,    1,    1,    0,    0,    0,    0,    1,    1,    0,    0,    0,
	    0,   25,    0,   42,   42,   42,   42,   42,   42,   42,   42,   42,
	    0,    0,   21,    0,   39,   39,    0,   27,    9,   27,    1,    1,
	    0,    1,    0,    1,    1,    1,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    3,    0,    0,    0,    5,    0,    0,
	    0,    7,    0,    7,    0,    7,    0,    0,    0,    1,   13,    1,
	    1,    0,    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    0,   19,    0,    1,   23,    1,    1,    1,    1,    1,
	    1,    1,    1,    0,    0,    0,    0,    1,    1,    0,    0,    0,
	    0,   25,    0,   42,   42,   42,   42,   42,   42,   42,   42,   42,
	    0,    0,   21,    0,   39,   39,    0,   27,    9,   27,    1,    1,
	    0,    1,    0,    1,    1,    1,    0,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    0,   11,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    0,    0,    0,    0,    1,   15,   15,   15,    1,
	    0,    0,   23,    1,    0,   33,   17,    0,   36,   30,    0,    0,
	   23,    1,    0,    0,    0,    0,    0,    1,    0,    1,   27,    9,
	   27,   27,    1,    1,    0,    1,    1,    1,    0,   48,   25,    0,
	   51,   42,    0,   21,   39,    0,   39,    1,    0,    1,    1,    1,
	    1,    0,    1,    1,    1,   19,    0,    0,    0,    0,    1,    0,
	    1,    1,   27,    9,   27,   27,    1,    1,    1,    0,    1,    1,
	    1,    1,    0,   66,   45,    0,   70,   62,    1,    0,    1,    1,
	    1,    1,    0,    1,    1,    1,    1,    0,    1,    1,    1,    1,
	    0,    1,    1,    1,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    3,    0,    0,    0,
	    5,    0,    0,    0,    0,    0,    0,   19,    0,    0,   23,    1,
	    0,    0,    0,    1,    0,    1,   27,    9,   27,   27,    1,    1,
	    0,    1,    1,    1,    0,   48,   25,    0,   51,   42,    0,   21,
	   39,    0,   39,    0,    0,    1,    0,    1,    1,   27,    9,   27,
	   27,    1,    1,    1,    0,    1,    1,    1,    1,    0,   66,   45,
	    0,   70,   62,    0,   19,    0,    0,   23,    1,    0,    0,    0,
	   19,    0,    0,   23,    1,    0,    0,    0,    1,    0,    1,   27,
	    9,   27,   27,    1,    1,    0,    1,    1,    1,    0,   48,   25,
	    0,   51,   42,    0,   21,   39,    0,   39,    0,    0,    1,    0,
	    1,    1,   27,    9,   27,   27,    1,    1,    1,    0,    1,    1,
	    1,    1,    0,   66,   45,    0,   70,   62,    0
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
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,   19,    0,   48,   54,   48,   48,   58,   48,   54,   48,   48,
	   19,    0,    0,   19,    0,   48,   54,   48,   48,   58,   48,   54,
	   48,   48
	};
}

private static final byte _http_eof_actions[] = init__http_eof_actions_0();


static final int http_start = 1;
static final int http_first_final = 121;
static final int http_error = 0;

static final int http_en_http_request = 40;
static final int http_en_http_response = 63;
static final int http_en_http_request_lenient = 87;
static final int http_en_http_response_lenient = 1;


// line 310 "HttpParser.rl"
}