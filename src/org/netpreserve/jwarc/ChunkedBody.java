
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

    public int read(ByteBuffer dst) throws IOException {
        while (chunkLength != 0) {
            if (!buffer.hasRemaining()) {
                // optimisation: let large reads bypass our buffer
                if (remaining >= buffer.capacity() && dst.remaining() >= buffer.capacity()) {
                    int n = channel.read(dst);
                    if (n < 0) throw new EOFException("EOF reached before end of chunked encoding");
                    remaining -= n;
                    position += n;
                    return n;
                }

                // refill
                ByteBuffer copy = buffer.duplicate();
                buffer.compact();
                if (channel.read(buffer) < 0) {
                    throw new EOFException("EOF reached before end of chunked encoding: "
                            + getErrorContext(copy, (int) copy.position(), 40));
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
        }
        return -1;
    }

    
// line 117 "ChunkedBody.rl"


    private int cs = chunked_start;
    private long tmp;

    private void parse() throws ParsingException {
        int p = buffer.position();
        int pe = buffer.limit();
        
// line 88 "ChunkedBody.java"
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
// line 78 "ChunkedBody.rl"
	{ tmp = tmp * 16 + Character.digit(buffer.get(p), 16); }
	break;
	case 1:
// line 79 "ChunkedBody.rl"
	{ if (tmp != 0) { chunkLength = tmp; tmp = 0; { p += 1; _goto_targ = 5; if (true)  continue _goto;} } }
	break;
	case 2:
// line 80 "ChunkedBody.rl"
	{ chunkLength = 0; }
	break;
// line 180 "ChunkedBody.java"
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

// line 126 "ChunkedBody.rl"
        buffer.position(p);
        if (cs == chunked_error) {
            throw new ParsingException("chunked encoding at position " + p + ": "
                    + getErrorContext(buffer, (int) p, 40));
        }
    }

    
// line 209 "ChunkedBody.java"
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
	    0,    0,    7,   16,   17,   33,   34,   42,   43,   44,   45,   60,
	   76,   91,  108,  115,  117,  123,  139,  145,  146,  164,  165,  180,
	  196,  211,  228,  235,  237,  243,  250
	};
}

private static final short _chunked_key_offsets[] = init__chunked_key_offsets_0();


private static char[] init__chunked_trans_keys_0()
{
	return new char [] {
	   48,   49,   57,   65,   70,   97,  102,   13,   48,   59,   49,   57,
	   65,   70,   97,  102,   10,   13,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,   13,   59,
	   48,   57,   65,   70,   97,  102,   10,   13,   10,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	   33,   61,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   34,  124,  126,   33,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,   13,   33,   59,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   34,   92,   32,  126,  128,  255,   13,   59,    0,    9,   11,
	   12,   14,  127,   33,   58,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,  126,  128,
	  255,   10,    9,   13,   32,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   10,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	   33,   61,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   34,  124,  126,   33,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,   13,   33,   59,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   34,   92,   32,  126,  128,  255,   13,   59,    0,    9,   11,
	   12,   14,  127,   48,   49,   57,   65,   70,   97,  102,    0
	};
}

private static final char _chunked_trans_keys[] = init__chunked_trans_keys_0();


private static byte[] init__chunked_single_lengths_0()
{
	return new byte [] {
	    0,    1,    3,    1,    4,    1,    2,    1,    1,    1,    3,    4,
	    3,    5,    3,    2,    0,    4,    2,    1,    6,    1,    3,    4,
	    3,    5,    3,    2,    0,    1,    0
	};
}

private static final byte _chunked_single_lengths[] = init__chunked_single_lengths_0();


private static byte[] init__chunked_range_lengths_0()
{
	return new byte [] {
	    0,    3,    3,    0,    6,    0,    3,    0,    0,    0,    6,    6,
	    6,    6,    2,    0,    3,    6,    2,    0,    6,    0,    6,    6,
	    6,    6,    2,    0,    3,    3,    0
	};
}

private static final byte _chunked_range_lengths[] = init__chunked_range_lengths_0();


private static short[] init__chunked_index_offsets_0()
{
	return new short [] {
	    0,    0,    5,   12,   14,   25,   27,   33,   35,   37,   39,   49,
	   60,   70,   82,   88,   91,   95,  106,  111,  113,  126,  128,  138,
	  149,  159,  171,  177,  180,  184,  189
	};
}

private static final short _chunked_index_offsets[] = init__chunked_index_offsets_0();


private static byte[] init__chunked_indicies_0()
{
	return new byte [] {
	    0,    2,    2,    2,    1,    3,    0,    4,    2,    2,    2,    1,
	    5,    1,    6,    7,    7,    7,    7,    7,    7,    7,    7,    7,
	    1,    8,    1,    9,   10,    2,    2,    2,    1,   11,    1,   12,
	    1,   13,    1,   14,   14,   14,   14,   14,   14,   14,   14,   14,
	    1,   14,   15,   14,   14,   14,   14,   14,   14,   14,   14,    1,
	   17,   16,   16,   16,   16,   16,   16,   16,   16,    1,    9,   16,
	   10,   16,   16,   16,   16,   16,   16,   16,   16,    1,   17,   18,
	   19,   17,   17,    1,    9,   10,    1,   17,   17,   17,    1,    7,
	   20,    7,    7,    7,    7,    7,    7,    7,    7,    1,   20,   21,
	   20,   20,    1,   22,    1,   20,   23,   20,    7,    7,    7,    7,
	    7,    7,    7,    7,    7,    1,   24,    1,   25,   25,   25,   25,
	   25,   25,   25,   25,   25,    1,   25,   26,   25,   25,   25,   25,
	   25,   25,   25,   25,    1,   28,   27,   27,   27,   27,   27,   27,
	   27,   27,    1,    3,   27,    4,   27,   27,   27,   27,   27,   27,
	   27,   27,    1,   28,   29,   30,   28,   28,    1,    3,    4,    1,
	   28,   28,   28,    1,    0,    2,    2,    2,    1,    1,    0
	};
}

private static final byte _chunked_indicies[] = init__chunked_indicies_0();


private static byte[] init__chunked_trans_targs_0()
{
	return new byte [] {
	    2,    0,    6,    3,   22,    4,    5,   17,   29,    7,   10,    8,
	    9,    1,   11,   12,   13,   14,   15,   16,   18,   19,   20,   21,
	   30,   23,   24,   25,   26,   27,   28
	};
}

private static final byte _chunked_trans_targs[] = init__chunked_trans_targs_0();


private static byte[] init__chunked_trans_actions_0()
{
	return new byte [] {
	    1,    0,    1,    0,    0,    3,    0,    0,    5,    0,    0,    3,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    5,    0,    0,    0,    0,    0,    0
	};
}

private static final byte _chunked_trans_actions[] = init__chunked_trans_actions_0();


static final int chunked_start = 1;
static final int chunked_first_final = 29;
static final int chunked_error = 0;

static final int chunked_en_chunks = 1;


// line 134 "ChunkedBody.rl"
}
