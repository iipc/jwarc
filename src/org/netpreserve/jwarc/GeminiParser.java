
// line 1 "GeminiParser.rl"
// recompile: ragel -J GeminiParser.rl -o GeminiParser.java
// diagram:   ragel -Vp GeminiParser.rl | dot -Tpng | feh -
// spec: https://gemini.circumlunar.space/docs/specification.gmi

// line 29 "GeminiParser.rl"


package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GeminiParser extends MessageParser {
    private int initialState;
    private int cs;
    private long position;
    private boolean finished;
    private byte[] buf = new byte[256];
    private int bufPos = 0;
    private int status;
    private String meta;
    private String url;

	public GeminiParser() {
        reset();
    }

    public void reset() {
        
// line 39 "GeminiParser.java"
	{
	cs = gemini_start;
	}

// line 59 "GeminiParser.rl"
        bufPos = 0;
        if (buf.length > 8192) {
            buf = new byte[256]; // if our buffer grew really big release it
        }
        status = 0;
        meta = null;
        url = null;
        position = 0;
        finished = false;
        cs = initialState;
    }

    public int status() {
        return status;
    }

    public String meta() {
        return meta;
    }

    public String url() {
        return url;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isError() {
        return cs == gemini_error;
    }

    /**
     * Configures the parser to read a gemini request while rejecting deviations from the standard.
     */
    public void strictRequest() {
        cs = gemini_en_gemini_request;
        initialState = cs;
    }

    /**
     * Configures the parser to read a gemini response while rejecting deviations from the standard.
     */
    public void strictResponse() {
        cs = gemini_en_gemini_response;
        initialState = cs;
    }

    /**
     * Runs the parser on a buffer of data. Passing null as the buffer indicates the end of input.
     */
    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) throws ParsingException {
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

        
// line 113 "GeminiParser.java"
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
	_keys = _gemini_key_offsets[cs];
	_trans = _gemini_index_offsets[cs];
	_klen = _gemini_single_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + _klen - 1;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + ((_upper-_lower) >> 1);
			if ( ( (data.get(p) & 0xff)) < _gemini_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( (data.get(p) & 0xff)) > _gemini_trans_keys[_mid] )
				_lower = _mid + 1;
			else {
				_trans += (_mid - _keys);
				break _match;
			}
		}
		_keys += _klen;
		_trans += _klen;
	}

	_klen = _gemini_range_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + (_klen<<1) - 2;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + (((_upper-_lower) >> 1) & ~1);
			if ( ( (data.get(p) & 0xff)) < _gemini_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( (data.get(p) & 0xff)) > _gemini_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

	cs = _gemini_trans_targs[_trans];

	if ( _gemini_trans_actions[_trans] != 0 ) {
		_acts = _gemini_trans_actions[_trans];
		_nacts = (int) _gemini_actions[_acts++];
		while ( _nacts-- > 0 )
	{
			switch ( _gemini_actions[_acts++] )
			{
	case 0:
// line 10 "GeminiParser.rl"
	{ push(data.get(p)); }
	break;
	case 1:
// line 11 "GeminiParser.rl"
	{ status = status * 10 + data.get(p) - '0'; }
	break;
	case 2:
// line 12 "GeminiParser.rl"
	{ meta = new String(buf, 0, bufPos, UTF_8); bufPos = 0; }
	break;
	case 3:
// line 13 "GeminiParser.rl"
	{ url = new String(buf, 0, bufPos, UTF_8); bufPos = 0; }
	break;
	case 4:
// line 14 "GeminiParser.rl"
	{ finished = true; { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
// line 212 "GeminiParser.java"
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

// line 127 "GeminiParser.rl"

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
                throw new ParsingException("invalid gemini message at byte position " + position + ": "
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

    private void push(byte b) throws ParsingException {
        if (bufPos >= 1024) throw new ParsingException("gemini header field longer than 1024 bytes");
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    
// line 279 "GeminiParser.java"
private static byte[] init__gemini_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4
	};
}

private static final byte _gemini_actions[] = init__gemini_actions_0();


private static byte[] init__gemini_key_offsets_0()
{
	return new byte [] {
	    0,    0,    2,    4,    5,    7,    8,   10,   11,   11
	};
}

private static final byte _gemini_key_offsets[] = init__gemini_key_offsets_0();


private static char[] init__gemini_trans_keys_0()
{
	return new char [] {
	   48,   57,   48,   57,   32,   10,   13,   10,   10,   13,   10,    0
	};
}

private static final char _gemini_trans_keys[] = init__gemini_trans_keys_0();


private static byte[] init__gemini_single_lengths_0()
{
	return new byte [] {
	    0,    0,    0,    1,    2,    1,    2,    1,    0,    0
	};
}

private static final byte _gemini_single_lengths[] = init__gemini_single_lengths_0();


private static byte[] init__gemini_range_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    0,    0,    0,    0,    0,    0,    0
	};
}

private static final byte _gemini_range_lengths[] = init__gemini_range_lengths_0();


private static byte[] init__gemini_index_offsets_0()
{
	return new byte [] {
	    0,    0,    2,    4,    6,    9,   11,   14,   16,   17
	};
}

private static final byte _gemini_index_offsets[] = init__gemini_index_offsets_0();


private static byte[] init__gemini_trans_targs_0()
{
	return new byte [] {
	    2,    0,    3,    0,    4,    0,    0,    5,    4,    8,    0,    0,
	    7,    6,    9,    0,    0,    0,    0
	};
}

private static final byte _gemini_trans_targs[] = init__gemini_trans_targs_0();


private static byte[] init__gemini_trans_actions_0()
{
	return new byte [] {
	    3,    0,    3,    0,    0,    0,    0,    5,    1,    9,    0,    0,
	    7,    1,    9,    0,    0,    0,    0
	};
}

private static final byte _gemini_trans_actions[] = init__gemini_trans_actions_0();


static final int gemini_start = 1;
static final int gemini_first_final = 8;
static final int gemini_error = 0;

static final int gemini_en_gemini_request = 6;
static final int gemini_en_gemini_response = 1;


// line 173 "GeminiParser.rl"
}