
// line 1 "WarcParser.rl"
// recompile: ragel -J WarcParser.rl -o WarcParser.java
// diagram:   ragel -Vp WarcParser.rl | dot -Tpng | feh -

package org.netpreserve.jwarc;

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
public class WarcParser extends MessageParser {
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

    public void parse(ByteBuffer data) {
        int p = data.position();
        int pe = data.limit();

        
// line 87 "WarcParser.java"
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
			if ( ( (data.get(p) & 0xff)) < _warc_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( (data.get(p) & 0xff)) > _warc_trans_keys[_mid] )
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
			if ( ( (data.get(p) & 0xff)) < _warc_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( (data.get(p) & 0xff)) > _warc_trans_keys[_mid+1] )
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
// line 26 "WarcParser.rl"
	{ push(data.get(p)); }
	break;
	case 1:
// line 27 "WarcParser.rl"
	{ major = major * 10 + data.get(p) - '0'; }
	break;
	case 2:
// line 28 "WarcParser.rl"
	{ minor = minor * 10 + data.get(p) - '0'; }
	break;
	case 3:
// line 29 "WarcParser.rl"
	{ endOfText = bufPos; }
	break;
	case 4:
// line 31 "WarcParser.rl"
	{
    if (bufPos > 0) {
        bufPos = endOfText;
        push((byte)' ');
    }
}
	break;
	case 5:
// line 38 "WarcParser.rl"
	{
    name = new String(buf, 0, bufPos, US_ASCII);
    bufPos = 0;
}
	break;
	case 6:
// line 43 "WarcParser.rl"
	{
    String value = new String(buf, 0, endOfText, UTF_8);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}
	break;
	case 7:
// line 50 "WarcParser.rl"
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
// line 68 "WarcParser.rl"
	{
    setHeader("WARC-IP-Address", new String(buf, 0, bufPos, US_ASCII));
    bufPos = 0;
}
	break;
	case 9:
// line 73 "WarcParser.rl"
	{
    String arcDate = new String(buf, 0, bufPos, US_ASCII);
    Instant instant = LocalDateTime.parse(arcDate, arcTimeFormat).toInstant(ZoneOffset.UTC);
    setHeader("WARC-Date", instant.toString());
    bufPos = 0;
}
	break;
	case 10:
// line 80 "WarcParser.rl"
	{
    // TODO
    bufPos = 0;
}
	break;
	case 11:
// line 85 "WarcParser.rl"
	{
    setHeader("Content-Length", new String(buf, 0, bufPos, US_ASCII));
    bufPos = 0;
}
	break;
	case 12:
// line 90 "WarcParser.rl"
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
// line 270 "WarcParser.java"
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

// line 208 "WarcParser.rl"

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
                throw new ParsingException("invalid WARC record at position " + position + ": "
                        + getErrorContext(buffer, (int) position, 40));
            }
            buffer.compact();
            int n = channel.read(buffer);
            buffer.flip();
            if (n < 0) {
                if (position > 0) {
                    throw new EOFException();
                }
                return false;
            }
        }
    }

    private void push(byte b) {
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    public MessageHeaders headers() {
        return new MessageHeaders(headerMap);
    }

    public MessageVersion version() {
        return new MessageVersion(protocol, major, minor);
    }

    public long position() {
        return position;
    }

    private void setHeader(String name, String value) {
        List<String> list = new ArrayList<>();
        list.add(value);
        headerMap.put(name, list);
    }

    
// line 344 "WarcParser.java"
private static byte[] init__warc_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   10,    1,
	   13,    2,    0,   10,    2,    3,    0,    2,    4,    0,    2,    6,
	    0,    3,   11,   12,   13
	};
}

private static final byte _warc_actions[] = init__warc_actions_0();


private static short[] init__warc_key_offsets_0()
{
	return new short [] {
	    0,    0,    3,    4,    5,    6,    7,    9,   12,   14,   17,   18,
	   34,   35,   51,   57,   58,   76,   82,   88,   94,   97,  111,  113,
	  116,  118,  121,  123,  126,  128,  131,  133,  135,  137,  139,  141,
	  143,  145,  147,  149,  151,  153,  155,  157,  159,  160,  176,  190,
	  205,  223,  226,  243,  259,  274,  281,  284,  289,  292,  296,  311,
	  326,  341,  356,  371,  386,  401,  403,  406,  407,  410,  411,  414,
	  415,  418,  419,  435,  436,  452,  458,  459,  477,  483,  489,  495,
	  495
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
	  122,   32,   33,   61,   93,   95,  126,   36,   59,   63,   91,   97,
	  122,  128,  255,   48,   57,   46,   48,   57,   48,   57,   46,   48,
	   57,   48,   57,   46,   48,   57,   48,   57,   32,   48,   57,   48,
	   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,
	   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,
	   57,   48,   57,   32,   33,  110,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   33,   47,  124,  126,
	   35,   39,   42,   43,   45,   57,   65,   90,   94,  122,   33,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   32,   33,   59,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,    9,   32,   59,    9,   32,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   33,   61,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,   34,  124,  126,   33,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   34,
	   92,   32,  126,  128,  255,    9,   32,   59,    9,   32,   59,   48,
	   57,   10,   48,   57,    0,  191,  194,  244,   33,   47,  111,  124,
	  126,   35,   39,   42,   43,   45,   57,   65,   90,   94,  122,   33,
	   45,   47,  124,  126,   35,   39,   42,   43,   46,   57,   65,   90,
	   94,  122,   33,   47,  116,  124,  126,   35,   39,   42,   43,   45,
	   57,   65,   90,   94,  122,   33,   47,  121,  124,  126,   35,   39,
	   42,   43,   45,   57,   65,   90,   94,  122,   33,   47,  112,  124,
	  126,   35,   39,   42,   43,   45,   57,   65,   90,   94,  122,   33,
	   47,  101,  124,  126,   35,   39,   42,   43,   45,   57,   65,   90,
	   94,  122,   32,   33,   47,  124,  126,   35,   39,   42,   43,   45,
	   57,   65,   90,   94,  122,   48,   57,   32,   48,   57,   32,   46,
	   48,   57,   46,   46,   48,   57,   46,   46,   48,   57,   46,   13,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   10,   33,   58,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,  127,
	    0,   31,   10,    9,   13,   32,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,
	  127,    0,   31,    9,   13,   32,  127,    0,   31,    9,   13,   32,
	  127,    0,   31,    0
	};
}

private static final char _warc_trans_keys[] = init__warc_trans_keys_0();


private static byte[] init__warc_single_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    0,    1,    1,    4,
	    1,    4,    4,    1,    6,    4,    4,    4,    1,    6,    0,    1,
	    0,    1,    0,    1,    0,    1,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    1,    4,    4,    3,
	    6,    3,    5,    4,    3,    3,    3,    3,    1,    0,    5,    5,
	    5,    5,    5,    5,    5,    0,    1,    1,    1,    1,    1,    1,
	    1,    1,    4,    1,    4,    4,    1,    6,    4,    4,    4,    0,
	    0
	};
}

private static final byte _warc_single_lengths[] = init__warc_single_lengths_0();


private static byte[] init__warc_range_lengths_0()
{
	return new byte [] {
	    0,    1,    0,    0,    0,    0,    1,    1,    1,    1,    0,    6,
	    0,    6,    1,    0,    6,    1,    1,    1,    1,    4,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    0,    6,    5,    6,
	    6,    0,    6,    6,    6,    2,    0,    1,    1,    2,    5,    5,
	    5,    5,    5,    5,    5,    1,    1,    0,    1,    0,    1,    0,
	    1,    0,    6,    0,    6,    1,    0,    6,    1,    1,    1,    0,
	    0
	};
}

private static final byte _warc_range_lengths[] = init__warc_range_lengths_0();


private static short[] init__warc_index_offsets_0()
{
	return new short [] {
	    0,    0,    3,    5,    7,    9,   11,   13,   16,   18,   21,   23,
	   34,   36,   47,   53,   55,   68,   74,   80,   86,   89,  100,  102,
	  105,  107,  110,  112,  115,  117,  120,  122,  124,  126,  128,  130,
	  132,  134,  136,  138,  140,  142,  144,  146,  148,  150,  161,  171,
	  181,  194,  198,  210,  221,  231,  237,  241,  246,  249,  252,  263,
	  274,  285,  296,  307,  318,  329,  331,  334,  336,  339,  341,  344,
	  346,  349,  351,  362,  364,  375,  381,  383,  396,  402,  408,  414,
	  415
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
	   28,   28,   28,    1,   30,    1,   31,   32,    1,   33,    1,   34,
	   35,    1,   36,    1,   37,   38,    1,   39,    1,   40,   41,    1,
	   42,    1,   43,    1,   44,    1,   45,    1,   46,    1,   47,    1,
	   48,    1,   49,    1,   50,    1,   51,    1,   52,    1,   53,    1,
	   54,    1,   55,    1,   56,    1,   57,   58,   57,   57,   57,   57,
	   57,   57,   57,   57,    1,   57,   59,   57,   57,   57,   57,   57,
	   57,   57,    1,   60,   60,   60,   60,   60,   60,   60,   60,   60,
	    1,   61,   62,   60,   63,   60,   60,   60,   60,   60,   60,   60,
	   60,    1,   61,   61,   63,    1,   63,   63,   64,   64,   64,   64,
	   64,   64,   64,   64,   64,    1,   64,   65,   64,   64,   64,   64,
	   64,   64,   64,   64,    1,   66,   60,   60,   60,   60,   60,   60,
	   60,   60,    1,   66,   67,   68,   66,   66,    1,   61,   62,   63,
	    1,   61,   61,   63,   69,    1,   70,   69,    1,   66,   66,    1,
	   57,   59,   71,   57,   57,   57,   57,   57,   57,   57,    1,   57,
	   72,   59,   57,   57,   57,   57,   57,   57,   57,    1,   57,   59,
	   73,   57,   57,   57,   57,   57,   57,   57,    1,   57,   59,   74,
	   57,   57,   57,   57,   57,   57,   57,    1,   57,   59,   75,   57,
	   57,   57,   57,   57,   57,   57,    1,   57,   59,   76,   57,   57,
	   57,   57,   57,   57,   57,    1,   77,   57,   59,   57,   57,   57,
	   57,   57,   57,   57,    1,   69,    1,   40,   78,    1,   40,    1,
	   37,   79,    1,   37,    1,   34,   80,    1,   34,    1,   31,   81,
	    1,   31,    1,   82,   83,   83,   83,   83,   83,   83,   83,   83,
	   83,    1,   84,    1,   83,   85,   83,   83,   83,   83,   83,   83,
	   83,   83,    1,   86,   87,   86,    1,    1,   88,   89,    1,   90,
	   91,   90,   92,   92,   92,   92,   92,   92,   92,   92,   92,    1,
	   90,   93,   90,    1,    1,   94,   95,   96,   95,    1,    1,   88,
	   97,   87,   97,    1,    1,   88,    1,    1,    0
	};
}

private static final byte _warc_indicies[] = init__warc_indicies_0();


private static byte[] init__warc_trans_targs_0()
{
	return new byte [] {
	    2,    0,   20,    3,    4,    5,    6,    7,    8,    9,   10,   11,
	   12,   13,   83,   14,   14,   15,   18,   16,   17,   12,   13,   15,
	   18,   19,   15,   19,   21,   22,   23,   24,   72,   25,   26,   70,
	   27,   28,   68,   29,   30,   66,   31,   32,   33,   34,   35,   36,
	   37,   38,   39,   40,   41,   42,   43,   44,   45,   46,   58,   47,
	   48,   49,   55,   50,   51,   52,   53,   54,   57,   56,   83,   59,
	   60,   61,   62,   63,   64,   65,   67,   69,   71,   73,   75,   76,
	   84,   77,   77,   78,   81,   79,   80,   75,   76,   78,   81,   82,
	   78,   82
	};
}

private static final byte _warc_trans_targs[] = init__warc_trans_targs_0();


private static byte[] init__warc_trans_actions_0()
{
	return new byte [] {
	    0,    0,    1,    0,    0,    0,    0,    3,    0,    5,    0,    0,
	    0,    1,   23,   11,    0,    0,    1,    0,    0,   13,   34,    9,
	   31,   28,    7,    1,    1,   15,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,   17,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,   19,    1,    1,    1,
	    1,    1,   25,    1,    1,    1,    1,    1,    1,    1,   37,    1,
	    1,    1,    1,    1,    1,   21,    1,    1,    1,    1,    0,    1,
	    0,   11,    0,    0,    1,    0,    0,   13,   34,    9,   31,   28,
	    7,    1
	};
}

private static final byte _warc_trans_actions[] = init__warc_trans_actions_0();


static final int warc_start = 1;
static final int warc_first_final = 83;
static final int warc_error = 0;

static final int warc_en_warc_fields = 74;
static final int warc_en_any_header = 1;


// line 261 "WarcParser.rl"
}