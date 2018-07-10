
// line 1 "WarcParser.rl"
// recompile: ragel -J WarcParser.rl -o WarcParser.java
// diagram:   ragel -Vp WarcParser.rl | dot -Tpng | feh -

package org.netpreserve.jwarc;

import javax.annotation.Generated;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.US_ASCII;


// line 146 "WarcParser.rl"


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
    private String protocol = "WARC";
    private Map<String,List<String>> headerMap;
    private static final DateTimeFormatter arcTimeFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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

        
// line 89 "WarcParser.java"
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
// line 27 "WarcParser.rl"
	{ push(data.get(p)); }
	break;
	case 1:
// line 28 "WarcParser.rl"
	{ major = major * 10 + data.get(p) - '0'; }
	break;
	case 2:
// line 29 "WarcParser.rl"
	{ minor = minor * 10 + data.get(p) - '0'; }
	break;
	case 3:
// line 30 "WarcParser.rl"
	{ endOfText = bufPos; }
	break;
	case 4:
// line 32 "WarcParser.rl"
	{
    if (bufPos > 0) {
        bufPos = endOfText;
        push((byte)' ');
    }
}
	break;
	case 5:
// line 39 "WarcParser.rl"
	{
    name = new String(buf, 0, bufPos, US_ASCII);
    bufPos = 0;
}
	break;
	case 6:
// line 44 "WarcParser.rl"
	{
    String value = new String(buf, 0, endOfText, UTF_8);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}
	break;
	case 7:
// line 51 "WarcParser.rl"
	{
    String url = new String(buf, 0, bufPos, ISO_8859_1);
    if (url.startsWith("filedesc://")) {
        setHeader("WARC-Type", "warcinfo");
        setHeader("WARC-Filename", url.substring("filedesc://".length()));
        setHeader("Content-Type", "text/plain");
    } else if (url.startsWith("dns:")) {
        setHeader("WARC-Type", "response");
        setHeader("Content-Type", "text/dns");
        setHeader("WARC-Target-URI", url);
     } else {
        setHeader("WARC-Type", "response");
        setHeader("Content-Type", "application/http;msgtype=response");
        setHeader("WARC-Target-URI", url);
    }
    bufPos = 0;
}
	break;
	case 8:
// line 69 "WarcParser.rl"
	{
    setHeader("WARC-IP-Address", new String(buf, 0, bufPos, US_ASCII));
    bufPos = 0;
}
	break;
	case 9:
// line 74 "WarcParser.rl"
	{
    String arcDate = new String(buf, 0, bufPos, US_ASCII);
    Instant instant = LocalDateTime.parse(arcDate, arcTimeFormat).toInstant(ZoneOffset.UTC);
    setHeader("WARC-Date", instant.toString());
    bufPos = 0;
}
	break;
	case 10:
// line 81 "WarcParser.rl"
	{
    // TODO
    bufPos = 0;
}
	break;
	case 11:
// line 86 "WarcParser.rl"
	{
    setHeader("Content-Length", new String(buf, 0, bufPos, US_ASCII));
    bufPos = 0;
}
	break;
	case 12:
// line 91 "WarcParser.rl"
	{
    protocol = "ARC";
    major = 1;
    minor = 1;
}
	break;
	case 13:
// line 144 "WarcParser.rl"
	{ { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
// line 272 "WarcParser.java"
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

// line 209 "WarcParser.rl"

        position += p - data.position();
        data.position(p);
    }

    public boolean parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (true) {
            parse(buffer);
            if (isFinished()) {
                return true;
            }
            if (isError()) {
                throw new ParsingException("invalid WARC record at position " + position);
            }
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
        return new ProtocolVersion(protocol, major, minor);
    }

    public long position() {
        return position;
    }

    private void setHeader(String name, String value) {
        List<String> list = new ArrayList<>();
        list.add(value);
        headerMap.put(name, list);
    }

    
// line 345 "WarcParser.java"
private static byte[] init__warc_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   13,    2,
	    0,   10,    2,    3,    0,    2,    4,    0,    2,    6,    0,    3,
	   11,   12,   13
	};
}

private static final byte _warc_actions[] = init__warc_actions_0();


private static short[] init__warc_key_offsets_0()
{
	return new short [] {
	    0,    0,    3,    4,    5,    6,    7,    9,   12,   14,   17,   18,
	   34,   35,   51,   57,   58,   76,   82,   88,   94,   97,  113,  115,
	  118,  120,  123,  125,  128,  130,  133,  135,  137,  139,  141,  143,
	  145,  147,  149,  151,  153,  155,  157,  159,  161,  162,  177,  191,
	  206,  224,  227,  244,  260,  275,  282,  285,  290,  293,  297,  300,
	  301,  304,  305,  308,  309,  312,  313,  329,  330,  346,  352,  353,
	  371,  377,  383,  389,  389
	};
}

private static final short _warc_key_offsets[] = init__warc_key_offsets_0();


private static char[] init__warc_trans_keys_0()
{
	return new char [] {
	   87,   97,  122,   65,   82,   67,   47,   48,   57,   46,   48,   57,
	   48,   57,   13,   48,   57,   10,   13,   33,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,   33,
	   58,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   13,   32,  127,    0,   31,   10,    9,   13,
	   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   13,   32,  127,    0,   31,    9,   13,
	   32,  127,    0,   31,    9,   13,   32,  127,    0,   31,   58,   97,
	  122,   32,   33,   36,   61,   95,  126,   38,   40,   42,   59,   63,
	   90,   97,  122,  128,  255,   48,   57,   46,   48,   57,   48,   57,
	   46,   48,   57,   48,   57,   46,   48,   57,   48,   57,   32,   48,
	   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,
	   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,
	   57,   48,   57,   48,   57,   32,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,   33,   47,  124,
	  126,   35,   39,   42,   43,   45,   57,   65,   90,   94,  122,   33,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,    9,   32,   33,   59,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,    9,   32,   59,    9,
	   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   33,   61,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   34,  124,  126,   33,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   34,   92,   32,  126,  128,  255,    9,   32,   59,    9,   32,   59,
	   48,   57,   10,   48,   57,    0,  191,  194,  244,   32,   48,   57,
	   32,   46,   48,   57,   46,   46,   48,   57,   46,   46,   48,   57,
	   46,   13,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,   10,   33,   58,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,
	   32,  127,    0,   31,   10,    9,   13,   32,   33,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,
	   13,   32,  127,    0,   31,    9,   13,   32,  127,    0,   31,    9,
	   13,   32,  127,    0,   31,    0
	};
}

private static final char _warc_trans_keys[] = init__warc_trans_keys_0();


private static byte[] init__warc_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    4,
	    1,    4,    4,    1,    6,    4,    4,    4,    1,    6,    0,    1,
	    0,    1,    0,    1,    0,    1,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    1,    3,    4,    3,
	    6,    3,    5,    4,    3,    3,    3,    3,    1,    0,    1,    1,
	    1,    1,    1,    1,    1,    1,    4,    1,    4,    4,    1,    6,
	    4,    4,    4,    0,    0
	};
}

private static final byte _warc_single_lengths[] = init__warc_single_lengths_0();


private static byte[] init__warc_range_lengths_0()
{
	return new byte [] {
	    0,    1,    0,    0,    0,    0,    1,    1,    1,    1,    0,    6,
	    0,    6,    1,    0,    6,    1,    1,    1,    1,    5,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    0,    6,    5,    6,
	    6,    0,    6,    6,    6,    2,    0,    1,    1,    2,    1,    0,
	    1,    0,    1,    0,    1,    0,    6,    0,    6,    1,    0,    6,
	    1,    1,    1,    0,    0
	};
}

private static final byte _warc_range_lengths[] = init__warc_range_lengths_0();


private static short[] init__warc_index_offsets_0()
{
	return new short [] {
	    0,    0,    3,    5,    7,    9,   11,   13,   16,   18,   21,   23,
	   34,   36,   47,   53,   55,   68,   74,   80,   86,   89,  101,  103,
	  106,  108,  111,  113,  116,  118,  121,  123,  125,  127,  129,  131,
	  133,  135,  137,  139,  141,  143,  145,  147,  149,  151,  161,  171,
	  181,  194,  198,  210,  221,  231,  237,  241,  246,  249,  252,  255,
	  257,  260,  262,  265,  267,  270,  272,  283,  285,  296,  302,  304,
	  317,  323,  329,  335,  336
	};
}

private static final short _warc_index_offsets[] = init__warc_index_offsets_0();


private static byte[] init__warc_indicies_0()
{
	return new byte [] {
	    0,    2,    1,    3,    1,    4,    1,    5,    1,    6,    1,    7,
	    1,    8,    7,    1,    9,    1,   10,    9,    1,   11,    1,   12,
	   13,   13,   13,   13,   13,   13,   13,   13,   13,    1,   14,    1,
	   13,   15,   13,   13,   13,   13,   13,   13,   13,   13,    1,   16,
	   17,   16,    1,    1,   18,   19,    1,   20,   21,   20,   22,   22,
	   22,   22,   22,   22,   22,   22,   22,    1,   20,   23,   20,    1,
	    1,   24,   25,   26,   25,    1,    1,   18,   27,   17,   27,    1,
	    1,   18,   28,    2,    1,   29,   28,   28,   28,   28,   28,   28,
	   28,   28,   28,   28,    1,   30,    1,   31,   32,    1,   33,    1,
	   34,   35,    1,   36,    1,   37,   38,    1,   39,    1,   40,   41,
	    1,   42,    1,   43,    1,   44,    1,   45,    1,   46,    1,   47,
	    1,   48,    1,   49,    1,   50,    1,   51,    1,   52,    1,   53,
	    1,   54,    1,   55,    1,   56,    1,   57,   57,   57,   57,   57,
	   57,   57,   57,   57,    1,   57,   58,   57,   57,   57,   57,   57,
	   57,   57,    1,   59,   59,   59,   59,   59,   59,   59,   59,   59,
	    1,   60,   61,   59,   62,   59,   59,   59,   59,   59,   59,   59,
	   59,    1,   60,   60,   62,    1,   62,   62,   63,   63,   63,   63,
	   63,   63,   63,   63,   63,    1,   63,   64,   63,   63,   63,   63,
	   63,   63,   63,   63,    1,   65,   59,   59,   59,   59,   59,   59,
	   59,   59,    1,   65,   66,   67,   65,   65,    1,   60,   61,   62,
	    1,   60,   60,   62,   68,    1,   69,   68,    1,   65,   65,    1,
	   40,   70,    1,   40,    1,   37,   71,    1,   37,    1,   34,   72,
	    1,   34,    1,   31,   73,    1,   31,    1,   74,   75,   75,   75,
	   75,   75,   75,   75,   75,   75,    1,   76,    1,   75,   77,   75,
	   75,   75,   75,   75,   75,   75,   75,    1,   78,   79,   78,    1,
	    1,   80,   81,    1,   82,   83,   82,   84,   84,   84,   84,   84,
	   84,   84,   84,   84,    1,   82,   85,   82,    1,    1,   86,   87,
	   88,   87,    1,    1,   80,   89,   79,   89,    1,    1,   80,    1,
	    1,    0
	};
}

private static final byte _warc_indicies[] = init__warc_indicies_0();


private static byte[] init__warc_trans_targs_0()
{
	return new byte [] {
	    2,    0,   20,    3,    4,    5,    6,    7,    8,    9,   10,   11,
	   12,   13,   75,   14,   14,   15,   18,   16,   17,   12,   13,   15,
	   18,   19,   15,   19,   21,   22,   23,   24,   64,   25,   26,   62,
	   27,   28,   60,   29,   30,   58,   31,   32,   33,   34,   35,   36,
	   37,   38,   39,   40,   41,   42,   43,   44,   45,   46,   47,   48,
	   49,   55,   50,   51,   52,   53,   54,   57,   56,   75,   59,   61,
	   63,   65,   67,   68,   76,   69,   69,   70,   73,   71,   72,   67,
	   68,   70,   73,   74,   70,   74
	};
}

private static final byte _warc_trans_targs[] = init__warc_trans_targs_0();


private static byte[] init__warc_trans_actions_0()
{
	return new byte [] {
	    0,    0,    1,    0,    0,    0,    0,    3,    0,    5,    0,    0,
	    0,    1,   21,   11,    0,    0,    1,    0,    0,   13,   32,    9,
	   29,   26,    7,    1,    1,   15,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,   17,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,   19,    1,    1,    1,
	    1,   23,    1,    1,    1,    1,    1,    1,    1,   35,    1,    1,
	    1,    1,    0,    1,    0,   11,    0,    0,    1,    0,    0,   13,
	   32,    9,   29,   26,    7,    1
	};
}

private static final byte _warc_trans_actions[] = init__warc_trans_actions_0();


static final int warc_start = 1;
static final int warc_first_final = 75;
static final int warc_error = 0;

static final int warc_en_warc_fields = 66;
static final int warc_en_any_header = 1;


// line 261 "WarcParser.rl"
}