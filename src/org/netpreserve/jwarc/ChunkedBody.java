
// line 1 "ChunkedBody.rl"
// recompile: ragel -J ChunkedBody.rl -o ChunkedBody.java
// diagram:   ragel -Vp ChunkedBody.rl | dot -Tpng | feh -

package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

class ChunkedBody extends MessageBody {
    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private long position = 0;
    private long remaining = 0;
    private long chunkLength = -1;
    private boolean finished;
    private boolean strict;
    private boolean passthrough;

    public ChunkedBody(ReadableByteChannel channel, ByteBuffer buffer) {
        this.channel = channel;
        this.buffer = buffer;
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    public void close() throws IOException {
        channel.close();
    }

    public long position() {
        return position;
    }

    public ChunkedBody strict() {
        strict = true;
        return this;
    }

    public int read(ByteBuffer dst) throws IOException {
        if (passthrough) {
            if (buffer.hasRemaining()) {
                int n = IOUtils.transfer(buffer, dst);
                position += n;
                return n;
            }
            int n = channel.read(dst);
            if (n > 0) {
                position += n;
            }
            return n;
        }

        while (chunkLength != 0) {
            if (!buffer.hasRemaining()) {
                // optimisation: let large reads bypass our buffer
                if (remaining >= buffer.capacity() && dst.remaining() >= buffer.capacity()) {
                    int n = IOUtils.transfer(channel, dst, remaining);
                    if (n < 0) throw new EOFException("EOF reached before end of chunked encoding");
                    remaining -= n;
                    position += n;
                    return n;
                }

                // refill
                buffer.compact();
                if (channel.read(buffer) < 0) {
                    throw new EOFException("EOF reached before end of chunked encoding");
                }
                buffer.flip();
            }

            // if we're in the middle of a chunk satisfy it from our buffer
            if (remaining > 0) {
                int n = IOUtils.transfer(buffer, dst, remaining);
                remaining -= n;
                position += n;
                return n;
            }

            // otherwise run the header parser
            chunkLength = -1;
            parse();
            remaining = chunkLength;
            if (passthrough) {
                return read(dst);
            }
        }
        return -1;
    }

    
// line 138 "ChunkedBody.rl"


    private int cs = chunked_start;
    private long tmp;

    private void parse() throws ParsingException {
        int p = buffer.position();
        int pe = buffer.limit();
        
// line 109 "ChunkedBody.java"
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
	_keys = _chunked_key_offsets[cs];
	_trans = _chunked_index_offsets[cs];
	_klen = _chunked_single_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + _klen - 1;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + ((_upper-_lower) >> 1);
			if ( ( buffer.get(p)) < _chunked_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( buffer.get(p)) > _chunked_trans_keys[_mid] )
				_lower = _mid + 1;
			else {
				_trans += (_mid - _keys);
				break _match;
			}
		}
		_keys += _klen;
		_trans += _klen;
	}

	_klen = _chunked_range_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + (_klen<<1) - 2;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + (((_upper-_lower) >> 1) & ~1);
			if ( ( buffer.get(p)) < _chunked_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( buffer.get(p)) > _chunked_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

	_trans = _chunked_indicies[_trans];
	cs = _chunked_trans_targs[_trans];

	if ( _chunked_trans_actions[_trans] != 0 ) {
		_acts = _chunked_trans_actions[_trans];
		_nacts = (int) _chunked_actions[_acts++];
		while ( _nacts-- > 0 )
	{
			switch ( _chunked_actions[_acts++] )
			{
	case 0:
// line 99 "ChunkedBody.rl"
	{ tmp = tmp * 16 + Character.digit(buffer.get(p), 16); }
	break;
	case 1:
// line 100 "ChunkedBody.rl"
	{ if (tmp != 0) { chunkLength = tmp; tmp = 0; { p += 1; _goto_targ = 5; if (true)  continue _goto;} } }
	break;
	case 2:
// line 101 "ChunkedBody.rl"
	{ chunkLength = 0; }
	break;
// line 201 "ChunkedBody.java"
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

// line 147 "ChunkedBody.rl"
        if (cs == chunked_error) {
            if (strict) {
                throw new ParsingException("chunked encoding at position " + p + ": "
                        + getErrorContext(buffer, (int) p, 40));
            } else {
                passthrough = true;
            }
        } else {
            buffer.position(p);
        }
    }

    
// line 235 "ChunkedBody.java"
private static byte[] init__chunked_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2
	};
}

private static final byte _chunked_actions[] = init__chunked_actions_0();


private static short[] init__chunked_key_offsets_0()
{
	return new short [] {
	    0,    0,    7,   18,   21,   22,   38,   39,   49,   52,   53,   54,
	   55,   70,   86,  101,  120,  127,  131,  137,  153,  159,  160,  178,
	  179,  194,  210,  225,  244,  251,  255,  261,  268
	};
}

private static final short _chunked_key_offsets[] = init__chunked_key_offsets_0();


private static char[] init__chunked_trans_keys_0()
{
	return new char [] {
	   48,   49,   57,   65,   70,   97,  102,    9,   13,   32,   48,   59,
	   49,   57,   65,   70,   97,  102,    9,   13,   32,   10,   13,   33,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   10,    9,   13,   32,   59,   48,   57,   65,   70,   97,
	  102,    9,   13,   32,   10,   13,   10,   33,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   33,   61,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   34,  124,  126,   33,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,    9,   13,   32,   33,   59,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   34,   92,   32,  126,  128,  255,    9,   13,   32,   59,    0,
	    9,   11,   12,   14,  127,   33,   58,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,
	  126,  128,  255,   10,    9,   13,   32,   33,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,   33,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   33,   61,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,   34,  124,  126,   33,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,
	   33,   59,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   34,   92,   32,  126,  128,  255,    9,
	   13,   32,   59,    0,    9,   11,   12,   14,  127,   48,   49,   57,
	   65,   70,   97,  102,    0
	};
}

private static final char _chunked_trans_keys[] = init__chunked_trans_keys_0();


private static byte[] init__chunked_single_lengths_0()
{
	return new byte [] {
	    0,    1,    5,    3,    1,    4,    1,    4,    3,    1,    1,    1,
	    3,    4,    3,    7,    3,    4,    0,    4,    2,    1,    6,    1,
	    3,    4,    3,    7,    3,    4,    0,    1,    0
	};
}

private static final byte _chunked_single_lengths[] = init__chunked_single_lengths_0();


private static byte[] init__chunked_range_lengths_0()
{
	return new byte [] {
	    0,    3,    3,    0,    0,    6,    0,    3,    0,    0,    0,    0,
	    6,    6,    6,    6,    2,    0,    3,    6,    2,    0,    6,    0,
	    6,    6,    6,    6,    2,    0,    3,    3,    0
	};
}

private static final byte _chunked_range_lengths[] = init__chunked_range_lengths_0();


private static short[] init__chunked_index_offsets_0()
{
	return new short [] {
	    0,    0,    5,   14,   18,   20,   31,   33,   41,   45,   47,   49,
	   51,   61,   72,   82,   96,  102,  107,  111,  122,  127,  129,  142,
	  144,  154,  165,  175,  189,  195,  200,  204,  209
	};
}

private static final short _chunked_index_offsets[] = init__chunked_index_offsets_0();


private static byte[] init__chunked_indicies_0()
{
	return new byte [] {
	    0,    2,    2,    2,    1,    3,    4,    3,    0,    5,    2,    2,
	    2,    1,    3,    4,    3,    1,    6,    1,    7,    8,    8,    8,
	    8,    8,    8,    8,    8,    8,    1,    9,    1,   10,   11,   10,
	   12,    2,    2,    2,    1,   10,   11,   10,    1,   13,    1,   14,
	    1,   15,    1,   16,   16,   16,   16,   16,   16,   16,   16,   16,
	    1,   16,   17,   16,   16,   16,   16,   16,   16,   16,   16,    1,
	   19,   18,   18,   18,   18,   18,   18,   18,   18,    1,   10,   11,
	   10,   18,   12,   18,   18,   18,   18,   18,   18,   18,   18,    1,
	   19,   20,   21,   19,   19,    1,   10,   11,   10,   12,    1,   19,
	   19,   19,    1,    8,   22,    8,    8,    8,    8,    8,    8,    8,
	    8,    1,   22,   23,   22,   22,    1,   24,    1,   22,   25,   22,
	    8,    8,    8,    8,    8,    8,    8,    8,    8,    1,   26,    1,
	   27,   27,   27,   27,   27,   27,   27,   27,   27,    1,   27,   28,
	   27,   27,   27,   27,   27,   27,   27,   27,    1,   30,   29,   29,
	   29,   29,   29,   29,   29,   29,    1,    3,    4,    3,   29,    5,
	   29,   29,   29,   29,   29,   29,   29,   29,    1,   30,   31,   32,
	   30,   30,    1,    3,    4,    3,    5,    1,   30,   30,   30,    1,
	    0,    2,    2,    2,    1,    1,    0
	};
}

private static final byte _chunked_indicies[] = init__chunked_indicies_0();


private static byte[] init__chunked_trans_targs_0()
{
	return new byte [] {
	    2,    0,    7,    3,    4,   24,    5,    6,   19,   31,    8,    9,
	   12,   10,   11,    1,   13,   14,   15,   16,   17,   18,   20,   21,
	   22,   23,   32,   25,   26,   27,   28,   29,   30
	};
}

private static final byte _chunked_trans_targs[] = init__chunked_trans_targs_0();


private static byte[] init__chunked_trans_actions_0()
{
	return new byte [] {
	    1,    0,    1,    0,    0,    0,    3,    0,    0,    5,    0,    0,
	    0,    3,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    5,    0,    0,    0,    0,    0,    0
	};
}

private static final byte _chunked_trans_actions[] = init__chunked_trans_actions_0();


static final int chunked_start = 1;
static final int chunked_first_final = 31;
static final int chunked_error = 0;

static final int chunked_en_chunks = 1;


// line 160 "ChunkedBody.rl"
}
