
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
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.US_ASCII;


// line 156 "WarcParser.rl"


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

        
// line 88 "WarcParser.java"
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
    // Some WARC files have been seen in the wild with truncated dates
    if (arcDate.length() < 14) {
        emitWarning("ARC date too short (" + arcDate.length() + " digits)");
        arcDate = arcDate + "00000000000000".substring(arcDate.length());
    } else if (arcDate.length() > 14) {
        emitWarning("ARC date too long (" + arcDate.length() + " digits)");
        arcDate = arcDate.substring(0, 14);
    }
    try {
        Instant instant = LocalDateTime.parse(arcDate, arcTimeFormat).toInstant(ZoneOffset.UTC);
        setHeader("WARC-Date", instant.toString());
    } catch (DateTimeParseException e) {
        emitWarning("ARC date not parsable");
    }
    bufPos = 0;
}
	break;
	case 10:
// line 93 "WarcParser.rl"
	{
    setHeader("Content-Length", new String(buf, 0, bufPos, US_ASCII));
    bufPos = 0;
}
	break;
	case 11:
// line 98 "WarcParser.rl"
	{
    protocol = "ARC";
    major = 1;
    minor = 1;
}
	break;
	case 12:
// line 154 "WarcParser.rl"
	{ { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
// line 276 "WarcParser.java"
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

// line 218 "WarcParser.rl"

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
                        + getErrorContext(buffer, buffer.position(), 40));
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

    
// line 350 "WarcParser.java"
private static byte[] init__warc_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   12,    2,
	    3,    0,    2,    4,    0,    2,    6,    0,    3,   10,   11,   12
	};
}

private static final byte _warc_actions[] = init__warc_actions_0();


private static short[] init__warc_key_offsets_0()
{
	return new short [] {
	    0,    0,    3,    4,    5,    6,    7,    9,   12,   14,   17,   18,
	   34,   35,   51,   57,   58,   76,   82,   88,   94,   97,   99,  101,
	  104,  106,  109,  111,  114,  116,  119,  121,  123,  125,  127,  129,
	  131,  133,  135,  138,  155,  157,  159,  162,  178,  195,  214,  218,
	  223,  226,  243,  259,  274,  292,  299,  302,  306,  324,  341,  358,
	  376,  393,  402,  413,  425,  431,  434,  437,  440,  443,  446,  449,
	  452,  455,  458,  461,  464,  467,  470,  473,  476,  479,  482,  485,
	  488,  489,  492,  493,  496,  497,  500,  501,  504,  505,  521,  522,
	  538,  544,  545,  563,  569,  575,  581,  581
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
	  122,   10,   32,   48,   57,   46,   48,   57,   48,   57,   46,   48,
	   57,   48,   57,   46,   48,   57,   48,   57,   32,   48,   57,   48,
	   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,
	   57,   48,   57,   32,   48,   57,   10,   32,   33,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,
	   32,   48,   57,   10,   48,   57,   10,   32,   33,   47,  124,  126,
	   35,   39,   42,   43,   45,   57,   65,   90,   94,  122,   10,   32,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   10,   32,   33,   59,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   10,
	   32,   59,    9,   32,   59,   48,   57,    9,   32,   59,    9,   32,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   33,   61,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,   34,  124,  126,   33,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   32,
	   33,   59,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   34,   92,   32,  126,  128,  255,    9,
	   32,   59,    0,  191,  194,  244,    9,   10,   32,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,
	   57,   65,   90,   94,  122,   10,   33,   61,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,   32,
	   33,   61,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   10,   32,   34,  124,  126,   33,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   10,   32,
	   34,   92,   33,  126,  128,  255,    9,   34,   92,   32,   47,   48,
	   57,   58,  126,  128,  255,    9,   10,   34,   92,   32,   47,   48,
	   57,   58,  126,  128,  255,   10,   32,    0,  191,  194,  244,   32,
	   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,
	   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,
	   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,
	   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,
	   48,   57,   32,   48,   57,   32,   48,   57,   32,   32,   48,   57,
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
	    1,    4,    4,    1,    6,    4,    4,    4,    1,    2,    0,    1,
	    0,    1,    0,    1,    0,    1,    0,    0,    0,    0,    0,    0,
	    0,    0,    1,    5,    2,    0,    1,    6,    5,    7,    4,    3,
	    3,    5,    4,    3,    6,    3,    3,    0,    6,    5,    5,    6,
	    5,    5,    3,    4,    2,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    4,    1,    4,
	    4,    1,    6,    4,    4,    4,    0,    0
	};
}

private static final byte _warc_single_lengths[] = init__warc_single_lengths_0();


private static byte[] init__warc_range_lengths_0()
{
	return new byte [] {
	    0,    1,    0,    0,    0,    0,    1,    1,    1,    1,    0,    6,
	    0,    6,    1,    0,    6,    1,    1,    1,    1,    0,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    6,    0,    1,    1,    5,    6,    6,    0,    1,
	    0,    6,    6,    6,    6,    2,    0,    2,    6,    6,    6,    6,
	    6,    2,    4,    4,    2,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    0,    1,    0,    1,    0,    1,    0,    1,    0,    6,    0,    6,
	    1,    0,    6,    1,    1,    1,    0,    0
	};
}

private static final byte _warc_range_lengths[] = init__warc_range_lengths_0();


private static short[] init__warc_index_offsets_0()
{
	return new short [] {
	    0,    0,    3,    5,    7,    9,   11,   13,   16,   18,   21,   23,
	   34,   36,   47,   53,   55,   68,   74,   80,   86,   89,   92,   94,
	   97,   99,  102,  104,  107,  109,  112,  114,  116,  118,  120,  122,
	  124,  126,  128,  131,  143,  146,  148,  151,  163,  175,  189,  194,
	  199,  203,  215,  226,  236,  249,  255,  259,  262,  275,  287,  299,
	  312,  324,  332,  340,  349,  354,  357,  360,  363,  366,  369,  372,
	  375,  378,  381,  384,  387,  390,  393,  396,  399,  402,  405,  408,
	  411,  413,  416,  418,  421,  423,  426,  428,  431,  433,  444,  446,
	  457,  463,  465,  478,  484,  490,  496,  497
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
	    1,   18,   28,    2,    1,    1,   29,   28,   30,    1,   31,   32,
	    1,   33,    1,   34,   35,    1,   36,    1,   37,   38,    1,   39,
	    1,   40,   41,    1,   42,    1,   43,    1,   44,    1,   45,    1,
	   46,    1,   47,    1,   48,    1,   49,    1,   50,   51,    1,    1,
	   53,   54,   54,   54,   54,   54,   54,   54,   54,   54,   52,    1,
	   53,   52,   55,    1,   56,   55,    1,    1,   53,   54,   57,   54,
	   54,   54,   54,   54,   54,   54,   52,    1,   53,   58,   58,   58,
	   58,   58,   58,   58,   58,   58,   52,   59,    1,   60,   58,   61,
	   58,   58,   58,   58,   58,   58,   58,   58,   52,   59,    1,   60,
	   61,   52,   62,   62,   63,   55,    1,   62,   62,   63,    1,   63,
	   63,   64,   64,   64,   64,   64,   64,   64,   64,   64,    1,   64,
	   65,   64,   64,   64,   64,   64,   64,   64,   64,    1,   67,   66,
	   66,   66,   66,   66,   66,   66,   66,    1,   62,   60,   66,   63,
	   66,   66,   66,   66,   66,   66,   66,   66,    1,   67,   68,   69,
	   67,   67,    1,   62,   60,   63,    1,   67,   67,    1,   61,    1,
	   70,   71,   71,   71,   71,   71,   71,   71,   71,   71,   52,   63,
	   63,   64,   64,   64,   64,   64,   64,   72,   64,   64,    1,   56,
	   64,   65,   64,   64,   64,   64,   64,   72,   64,   64,    1,    1,
	   53,   71,   73,   71,   71,   71,   71,   71,   71,   71,   71,   52,
	    1,   53,   74,   58,   58,   58,   58,   58,   58,   58,   58,   52,
	   74,    1,   75,   59,   76,   74,   74,   52,   67,   68,   69,   67,
	   77,   67,   67,    1,   67,   56,   68,   69,   67,   77,   67,   67,
	    1,   67,   75,   74,   74,   52,   50,   78,    1,   50,   79,    1,
	   50,   80,    1,   50,   81,    1,   50,   82,    1,   50,   83,    1,
	   50,   84,    1,   50,   85,    1,   50,   86,    1,   50,   87,    1,
	   50,   88,    1,   50,   89,    1,   50,   90,    1,   50,   91,    1,
	   50,   92,    1,   50,   93,    1,   50,   94,    1,   50,   95,    1,
	   50,   96,    1,   50,    1,   40,   97,    1,   40,    1,   37,   98,
	    1,   37,    1,   34,   99,    1,   34,    1,   31,  100,    1,   31,
	    1,  101,  102,  102,  102,  102,  102,  102,  102,  102,  102,    1,
	  103,    1,  102,  104,  102,  102,  102,  102,  102,  102,  102,  102,
	    1,  105,  106,  105,    1,    1,  107,  108,    1,  109,  110,  109,
	  111,  111,  111,  111,  111,  111,  111,  111,  111,    1,  109,  112,
	  109,    1,    1,  113,  114,  115,  114,    1,    1,  107,  116,  106,
	  116,    1,    1,  107,    1,    1,    0
	};
}

private static final byte _warc_indicies[] = init__warc_indicies_0();


private static byte[] init__warc_trans_targs_0()
{
	return new byte [] {
	    2,    0,   20,    3,    4,    5,    6,    7,    8,    9,   10,   11,
	   12,   13,  102,   14,   14,   15,   18,   16,   17,   12,   13,   15,
	   18,   19,   15,   19,   21,   22,   23,   24,   91,   25,   26,   89,
	   27,   28,   87,   29,   30,   85,   31,   32,   33,   34,   35,   36,
	   37,   38,   39,   65,   40,   41,   43,   42,  102,   44,   45,   46,
	   47,   56,   48,   49,   50,   51,   52,   53,   54,   55,   57,   59,
	   58,   60,   61,   62,   64,   63,   66,   67,   68,   69,   70,   71,
	   72,   73,   74,   75,   76,   77,   78,   79,   80,   81,   82,   83,
	   84,   86,   88,   90,   92,   94,   95,  103,   96,   96,   97,  100,
	   98,   99,   94,   95,   97,  100,  101,   97,  101
	};
}

private static final byte _warc_trans_targs[] = init__warc_trans_targs_0();


private static byte[] init__warc_trans_actions_0()
{
	return new byte [] {
	    0,    0,    1,    0,    0,    0,    0,    3,    0,    5,    0,    0,
	    0,    1,   21,   11,    0,    0,    1,    0,    0,   13,   29,    9,
	   26,   23,    7,    1,    1,   15,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,   17,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,   19,    1,    0,    0,    0,    1,   32,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    1,    0,    0,    0,    0,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    0,    1,    0,   11,    0,    0,    1,
	    0,    0,   13,   29,    9,   26,   23,    7,    1
	};
}

private static final byte _warc_trans_actions[] = init__warc_trans_actions_0();


static final int warc_start = 1;
static final int warc_first_final = 102;
static final int warc_error = 0;

static final int warc_en_warc_fields = 93;
static final int warc_en_any_header = 1;


// line 271 "WarcParser.rl"
}