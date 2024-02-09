
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


// line 169 "WarcParser.rl"


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
    bufPos = 0;
}
	break;
	case 12:
// line 102 "WarcParser.rl"
	{
    protocol = "ARC";
    major = 1;
    minor = 1;
}
	break;
	case 13:
// line 167 "WarcParser.rl"
	{ { p += 1; _goto_targ = 5; if (true)  continue _goto;} }
	break;
// line 282 "WarcParser.java"
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

// line 231 "WarcParser.rl"

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

    
// line 356 "WarcParser.java"
private static byte[] init__warc_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,    3,    1,    4,    1,
	    5,    1,    6,    1,    7,    1,    8,    1,    9,    1,   11,    1,
	   13,    2,    3,    0,    2,    4,    0,    2,    6,    0,    3,   10,
	   12,   13
	};
}

private static final byte _warc_actions[] = init__warc_actions_0();


private static short[] init__warc_key_offsets_0()
{
	return new short [] {
	    0,    0,    4,    7,   10,   12,   15,   17,   20,   24,   26,   28,
	   30,   32,   34,   36,   38,   40,   43,   60,   62,   64,   67,   70,
	   74,   76,   78,   80,   82,   84,   87,   89,   91,   93,   96,  112,
	  129,  148,  152,  157,  160,  177,  193,  208,  226,  233,  236,  240,
	  258,  275,  292,  309,  327,  344,  362,  379,  388,  399,  411,  423,
	  436,  445,  454,  463,  472,  483,  495,  504,  513,  524,  536,  540,
	  544,  549,  567,  584,  602,  619,  638,  644,  648,  652,  657,  675,
	  692,  709,  727,  744,  763,  768,  772,  776,  780,  782,  784,  788,
	  792,  796,  802,  806,  810,  814,  832,  850,  868,  885,  904,  910,
	  914,  918,  922,  926,  930,  932,  934,  936,  942,  948,  951,  954,
	  957,  960,  963,  966,  969,  972,  975,  978,  981,  984,  987,  990,
	  993,  996,  999, 1002, 1005, 1006, 1008, 1011, 1013, 1016, 1018, 1021,
	 1024, 1025, 1028, 1029, 1032, 1033, 1036, 1037, 1040, 1041, 1042, 1043,
	 1044, 1046, 1049, 1051, 1054, 1055, 1071, 1072, 1088, 1094, 1095, 1113,
	 1119, 1125, 1131, 1147, 1148, 1164, 1170, 1171, 1189, 1195, 1201, 1207,
	 1207
	};
}

private static final short _warc_key_offsets[] = init__warc_key_offsets_0();


private static char[] init__warc_trans_keys_0()
{
	return new char [] {
	   10,   87,   97,  122,   10,   97,  122,   10,   97,  122,   97,  122,
	   58,   97,  122,   10,   32,   48,   49,   57,   32,   46,   48,   57,
	   48,   57,   48,   57,   48,   57,   48,   57,   48,   57,   48,   57,
	   48,   57,   48,   57,   32,   48,   57,   10,   32,   33,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	   10,   32,   48,   57,   10,   48,   57,   10,   48,   57,   10,   32,
	   48,   57,   10,   32,   10,   32,   10,   32,   10,   32,   48,   57,
	   32,   48,   57,   10,   32,   10,   32,   48,   57,   10,   48,   57,
	   10,   32,   33,   47,  124,  126,   35,   39,   42,   43,   45,   57,
	   65,   90,   94,  122,   10,   32,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   10,   32,
	   33,   59,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   10,   32,   59,    9,   32,   59,   48,
	   57,    9,   32,   59,    9,   32,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,   33,   61,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,   34,  124,  126,   33,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   32,   33,   59,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   34,
	   92,   32,  126,  128,  255,    9,   32,   59,    0,  191,  194,  244,
	    9,   10,   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,
	   48,   57,   65,   90,   94,  122,    9,   32,   33,  124,  126,   35,
	   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,
	   33,   61,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   10,   33,   61,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,   32,   33,
	   61,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   10,   33,   61,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   10,   32,   33,   61,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   10,   32,   34,  124,  126,   33,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,    9,   10,   32,   34,   92,
	   33,  126,  128,  255,    9,   34,   92,   32,   47,   48,   57,   58,
	  126,  128,  255,    9,   10,   34,   92,   32,   47,   48,   57,   58,
	  126,  128,  255,    9,   10,   34,   92,   32,   47,   48,   57,   58,
	  126,  128,  255,    9,   10,   32,   34,   92,   33,   47,   48,   57,
	   58,  126,  128,  255,    9,   10,   32,   34,   92,   33,  126,  128,
	  255,    9,   10,   32,   34,   92,   33,  126,  128,  255,    9,   10,
	   32,   34,   92,   33,  126,  128,  255,    9,   10,   32,   34,   92,
	   33,  126,  128,  255,    9,   34,   92,   32,   47,   48,   57,   58,
	  126,  128,  255,    9,   32,   34,   92,   33,   47,   48,   57,   58,
	  126,  128,  255,    9,   10,   32,   34,   92,   33,  126,  128,  255,
	    9,   10,   32,   34,   92,   33,  126,  128,  255,    9,   34,   92,
	   32,   47,   48,   57,   58,  126,  128,  255,    9,   10,   34,   92,
	   32,   47,   48,   57,   58,  126,  128,  255,    9,   10,   32,   59,
	    9,   10,   32,   59,    9,   32,   59,   48,   57,    9,   10,   32,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   32,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   10,   32,   33,   61,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   10,   32,   34,  124,  126,   33,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,    9,   10,   32,   33,   59,
	  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,
	   94,  122,   10,   32,    0,  191,  194,  244,    9,   10,   32,   59,
	    9,   10,   32,   59,    9,   32,   59,   48,   57,    9,   10,   32,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,    9,   32,   33,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,   32,   33,   61,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,   10,   32,   33,   61,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,   10,   32,   34,  124,  126,
	   33,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   10,   32,   33,   59,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,    9,   32,   59,   48,   57,
	   10,   32,   48,   57,   10,   32,   48,   57,   10,   32,   48,   57,
	   10,   32,   10,   32,   10,   32,   48,   57,   10,   32,   48,   57,
	   10,   32,   48,   57,   10,   32,    0,  191,  194,  244,    9,   10,
	   32,   59,    9,   10,   32,   59,    9,   10,   32,   59,    9,   10,
	   32,   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   10,   32,   33,  124,  126,   35,   39,
	   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,   10,   32,
	   33,   61,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,   10,   32,   34,  124,  126,   33,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   10,   32,
	   33,   59,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,
	   65,   90,   94,  122,    9,   10,   32,   59,   48,   57,   10,   32,
	   48,   57,   10,   32,   48,   57,   10,   32,   48,   57,   10,   32,
	   48,   57,   10,   32,   48,   57,   10,   32,   10,   32,   48,   57,
	   10,   32,    0,  191,  194,  244,   10,   32,    0,  191,  194,  244,
	   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,
	   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,
	   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,
	   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,
	   32,   48,   57,   32,   48,   57,   32,   48,   57,   32,   48,   57,
	   46,   48,   57,   48,   57,   46,   48,   57,   48,   57,   32,   48,
	   57,   32,   48,   57,   32,   46,   48,   57,   46,   46,   48,   57,
	   46,   46,   48,   57,   46,   46,   48,   57,   65,   82,   67,   47,
	   48,   57,   46,   48,   57,   48,   57,   13,   48,   57,   10,   13,
	   33,  124,  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,
	   90,   94,  122,   10,   33,   58,  124,  126,   35,   39,   42,   43,
	   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,  127,
	    0,   31,   10,    9,   13,   32,   33,  124,  126,   35,   39,   42,
	   43,   45,   46,   48,   57,   65,   90,   94,  122,    9,   13,   32,
	  127,    0,   31,    9,   13,   32,  127,    0,   31,    9,   13,   32,
	  127,    0,   31,   13,   33,  124,  126,   35,   39,   42,   43,   45,
	   46,   48,   57,   65,   90,   94,  122,   10,   33,   58,  124,  126,
	   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,  122,
	    9,   13,   32,  127,    0,   31,   10,    9,   13,   32,   33,  124,
	  126,   35,   39,   42,   43,   45,   46,   48,   57,   65,   90,   94,
	  122,    9,   13,   32,  127,    0,   31,    9,   13,   32,  127,    0,
	   31,    9,   13,   32,  127,    0,   31,    0
	};
}

private static final char _warc_trans_keys[] = init__warc_trans_keys_0();


private static byte[] init__warc_single_lengths_0()
{
	return new byte [] {
	    0,    2,    1,    1,    0,    1,    2,    1,    2,    0,    0,    0,
	    0,    0,    0,    0,    0,    1,    5,    2,    0,    1,    1,    2,
	    2,    2,    2,    2,    0,    1,    2,    2,    0,    1,    6,    5,
	    7,    4,    3,    3,    5,    4,    3,    6,    3,    3,    0,    6,
	    5,    5,    5,    6,    5,    6,    5,    5,    3,    4,    4,    5,
	    5,    5,    5,    5,    3,    4,    5,    5,    3,    4,    4,    4,
	    3,    6,    5,    6,    5,    7,    2,    4,    4,    3,    6,    5,
	    5,    6,    5,    7,    3,    2,    2,    2,    2,    2,    2,    2,
	    2,    2,    4,    4,    4,    6,    6,    6,    5,    7,    4,    2,
	    2,    2,    2,    2,    2,    2,    0,    2,    2,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    0,    1,    0,    1,    0,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    0,    1,    0,    1,    1,    4,    1,    4,    4,    1,    6,    4,
	    4,    4,    4,    1,    4,    4,    1,    6,    4,    4,    4,    0,
	    0
	};
}

private static final byte _warc_single_lengths[] = init__warc_single_lengths_0();


private static byte[] init__warc_range_lengths_0()
{
	return new byte [] {
	    0,    1,    1,    1,    1,    1,    0,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    6,    0,    1,    1,    1,    1,
	    0,    0,    0,    0,    1,    1,    0,    0,    1,    1,    5,    6,
	    6,    0,    1,    0,    6,    6,    6,    6,    2,    0,    2,    6,
	    6,    6,    6,    6,    6,    6,    6,    2,    4,    4,    4,    4,
	    2,    2,    2,    2,    4,    4,    2,    2,    4,    4,    0,    0,
	    1,    6,    6,    6,    6,    6,    2,    0,    0,    1,    6,    6,
	    6,    6,    6,    6,    1,    1,    1,    1,    0,    0,    1,    1,
	    1,    2,    0,    0,    0,    6,    6,    6,    6,    6,    1,    1,
	    1,    1,    1,    1,    0,    0,    1,    2,    2,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    0,    1,    1,    1,    1,    1,    1,    1,
	    0,    1,    0,    1,    0,    1,    0,    1,    0,    0,    0,    0,
	    1,    1,    1,    1,    0,    6,    0,    6,    1,    0,    6,    1,
	    1,    1,    6,    0,    6,    1,    0,    6,    1,    1,    1,    0,
	    0
	};
}

private static final byte _warc_range_lengths[] = init__warc_range_lengths_0();


private static short[] init__warc_index_offsets_0()
{
	return new short [] {
	    0,    0,    4,    7,   10,   12,   15,   18,   21,   25,   27,   29,
	   31,   33,   35,   37,   39,   41,   44,   56,   59,   61,   64,   67,
	   71,   74,   77,   80,   83,   85,   88,   91,   94,   96,   99,  111,
	  123,  137,  142,  147,  151,  163,  174,  184,  197,  203,  207,  210,
	  223,  235,  247,  259,  272,  284,  297,  309,  317,  325,  334,  343,
	  353,  361,  369,  377,  385,  393,  402,  410,  418,  426,  435,  440,
	  445,  450,  463,  475,  488,  500,  514,  519,  524,  529,  534,  547,
	  559,  571,  584,  596,  610,  615,  619,  623,  627,  630,  633,  637,
	  641,  645,  650,  655,  660,  665,  678,  691,  704,  716,  730,  736,
	  740,  744,  748,  752,  756,  759,  762,  764,  769,  774,  777,  780,
	  783,  786,  789,  792,  795,  798,  801,  804,  807,  810,  813,  816,
	  819,  822,  825,  828,  831,  833,  835,  838,  840,  843,  845,  848,
	  851,  853,  856,  858,  861,  863,  866,  868,  871,  873,  875,  877,
	  879,  881,  884,  886,  889,  891,  902,  904,  915,  921,  923,  936,
	  942,  948,  954,  965,  967,  978,  984,  986,  999, 1005, 1011, 1017,
	 1018
	};
}

private static final short _warc_index_offsets[] = init__warc_index_offsets_0();


private static short[] init__warc_indicies_0()
{
	return new short [] {
	    0,    2,    3,    1,    4,    3,    1,    5,    3,    1,    3,    1,
	    6,    3,    1,    1,    7,    6,    8,    9,    1,   10,   11,   12,
	    1,   13,    1,   14,    1,   15,    1,   16,    1,   17,    1,   18,
	    1,   19,    1,   20,    1,   21,   22,    1,    1,   24,   25,   25,
	   25,   25,   25,   25,   25,   25,   25,   23,    1,   24,   23,   26,
	    1,   27,   28,    1,   27,   29,    1,   27,   30,   31,    1,    1,
	    1,   32,    1,   33,   32,    1,    1,   34,    1,   35,   34,   36,
	    1,   37,   36,    1,    1,    1,   38,    1,   39,   38,   31,    1,
	   27,   31,    1,    1,   24,   25,   40,   25,   25,   25,   25,   25,
	   25,   25,   23,    1,   24,   41,   41,   41,   41,   41,   41,   41,
	   41,   41,   23,   42,    1,   43,   41,   44,   41,   41,   41,   41,
	   41,   41,   41,   41,   23,   42,    1,   43,   44,   23,   45,   45,
	   46,   26,    1,   45,   45,   46,    1,   46,   46,   47,   47,   47,
	   47,   47,   47,   47,   47,   47,    1,   47,   48,   47,   47,   47,
	   47,   47,   47,   47,   47,    1,   50,   49,   49,   49,   49,   49,
	   49,   49,   49,    1,   45,   43,   49,   46,   49,   49,   49,   49,
	   49,   49,   49,   49,    1,   50,   51,   52,   50,   50,    1,   45,
	   43,   46,    1,   50,   50,    1,   44,    1,   53,   54,   54,   54,
	   54,   54,   54,   54,   54,   54,   23,   46,   46,   47,   47,   47,
	   47,   47,   47,   55,   47,   47,    1,   27,   47,   48,   47,   47,
	   47,   47,   47,   56,   47,   47,    1,   27,   47,   48,   47,   47,
	   47,   47,   47,   57,   47,   47,    1,   27,   30,   47,   48,   47,
	   47,   47,   47,   47,   58,   47,   47,    1,   27,   47,   48,   47,
	   47,   47,   47,   47,   58,   47,   47,    1,    1,   24,   54,   59,
	   54,   54,   54,   54,   54,   54,   54,   54,   23,    1,   24,   60,
	   41,   41,   41,   41,   41,   41,   41,   41,   23,   60,    1,   61,
	   42,   62,   60,   60,   23,   50,   51,   52,   50,   63,   50,   50,
	    1,   50,   27,   51,   52,   50,   64,   50,   50,    1,   50,   27,
	   51,   52,   50,   65,   50,   50,    1,   50,   27,   66,   51,   52,
	   50,   67,   50,   50,    1,   68,    1,   50,   69,   70,   68,   68,
	   32,   68,    1,   71,   69,   70,   68,   68,   32,   72,    1,   50,
	   73,   74,   72,   72,   34,   72,    1,   75,   73,   74,   72,   72,
	   34,   50,   51,   52,   50,   76,   50,   50,    1,   50,   77,   51,
	   52,   50,   76,   50,   50,    1,   78,    1,   50,   79,   80,   78,
	   78,   38,   78,    1,   81,   79,   80,   78,   78,   38,   50,   51,
	   52,   50,   67,   50,   50,    1,   50,   27,   51,   52,   50,   67,
	   50,   50,    1,   82,    1,   43,   83,   38,   82,    1,   84,   83,
	   38,   45,   45,   46,   31,    1,   83,    1,   85,   86,   86,   86,
	   86,   86,   86,   86,   86,   86,   38,   46,   46,   47,   47,   47,
	   47,   47,   47,   58,   47,   47,    1,    1,   39,   86,   87,   86,
	   86,   86,   86,   86,   86,   86,   86,   38,    1,   39,   78,   88,
	   88,   88,   88,   88,   88,   88,   88,   38,   82,    1,   43,   88,
	   83,   88,   88,   88,   88,   88,   88,   88,   88,   38,   50,   81,
	   78,   78,   38,   89,    1,   90,   91,   34,   89,    1,   92,   91,
	   34,   45,   45,   46,   36,    1,   91,    1,   93,   94,   94,   94,
	   94,   94,   94,   94,   94,   94,   34,   46,   46,   47,   47,   47,
	   47,   47,   47,   95,   47,   47,    1,   37,   47,   48,   47,   47,
	   47,   47,   47,   95,   47,   47,    1,    1,   35,   94,   96,   94,
	   94,   94,   94,   94,   94,   94,   94,   34,    1,   35,   72,   97,
	   97,   97,   97,   97,   97,   97,   97,   34,   89,    1,   90,   97,
	   91,   97,   97,   97,   97,   97,   97,   97,   97,   34,   45,   45,
	   46,   98,    1,   27,   37,   99,    1,   27,   37,  100,    1,   27,
	  101,  102,    1,    1,    1,  103,    1,  104,  103,    1,    1,  105,
	   34,   27,   35,  105,   34,   27,   37,  102,    1,   50,   75,   72,
	   72,   34,  106,    1,  107,  108,   32,  106,    1,  109,  108,   32,
	   89,    1,   45,   91,   34,  108,    1,  110,  111,  111,  111,  111,
	  111,  111,  111,  111,  111,   32,   91,    1,   46,   94,   94,   94,
	   94,   94,   94,   94,   94,   94,   34,    1,   33,  111,  112,  111,
	  111,  111,  111,  111,  111,  111,  111,   32,    1,   33,   68,  113,
	  113,  113,  113,  113,  113,  113,  113,   32,  106,    1,  107,  113,
	  108,  113,  113,  113,  113,  113,  113,  113,  113,   32,   89,    1,
	   45,   91,  114,   34,   27,   35,  115,   34,   27,   35,  116,   34,
	   27,  117,  105,   34,    1,    1,  118,   32,    1,  119,  118,   32,
	    1,    1,  120,    1,  121,  120,  102,    1,   50,   71,   68,   68,
	   32,   50,   61,   60,   60,   23,   21,  122,    1,   21,  123,    1,
	   21,  124,    1,   21,  125,    1,   21,  126,    1,   21,  127,    1,
	   21,  128,    1,   21,  129,    1,   21,  130,    1,   21,  131,    1,
	   21,  132,    1,   21,  133,    1,   21,  134,    1,   21,  135,    1,
	   21,  136,    1,   21,  137,    1,   21,  138,    1,   21,  139,    1,
	   21,  140,    1,   21,    1,  141,    1,  142,  143,    1,  144,    1,
	  145,  146,    1,  147,    1,   10,  148,    1,   10,  149,    1,   10,
	    1,  145,  150,    1,  145,    1,  142,  151,    1,  142,    1,   11,
	  152,    1,   11,    1,   11,   12,    1,  153,    1,  154,    1,  155,
	    1,  156,    1,  157,    1,  158,  157,    1,  159,    1,  160,  159,
	    1,  161,    1,  162,  163,  163,  163,  163,  163,  163,  163,  163,
	  163,    1,  164,    1,  163,  165,  163,  163,  163,  163,  163,  163,
	  163,  163,    1,  166,  167,  166,    1,    1,  168,  169,    1,  170,
	  171,  170,  172,  172,  172,  172,  172,  172,  172,  172,  172,    1,
	  170,  173,  170,    1,    1,  174,  175,  176,  175,    1,    1,  168,
	  177,  167,  177,    1,    1,  168,  178,  179,  179,  179,  179,  179,
	  179,  179,  179,  179,    1,  180,    1,  179,  181,  179,  179,  179,
	  179,  179,  179,  179,  179,    1,  182,  183,  182,    1,    1,  184,
	  185,    1,  186,  187,  186,  188,  188,  188,  188,  188,  188,  188,
	  188,  188,    1,  186,  189,  186,    1,    1,  190,  191,  192,  191,
	    1,    1,  184,  193,  183,  193,    1,    1,  184,    1,    1,    0
	};
}

private static final short _warc_indicies[] = init__warc_indicies_0();


private static short[] init__warc_trans_targs_0()
{
	return new short [] {
	    2,    0,  152,    5,    3,    4,    6,    7,    8,  151,    9,  137,
	  149,   10,   11,   12,   13,   14,   15,   16,   17,   18,  117,   19,
	   20,   34,   21,  179,   22,   23,   24,   33,   25,   26,   27,   28,
	   29,   30,   31,   32,   35,   36,   37,   38,   47,   39,   40,   41,
	   42,   43,   44,   45,   46,   48,   53,   49,   50,   51,   52,   54,
	   55,   56,  116,   57,   58,   59,   60,   69,   61,   98,  115,   62,
	   63,   79,   97,   64,   65,   66,   67,   70,   78,   68,   71,   73,
	   72,   74,   75,   76,   77,   80,   88,   82,   81,   83,   85,   84,
	   86,   87,   89,   90,   91,   92,   96,   93,   94,   95,   99,  106,
	  101,  100,  102,  103,  104,  105,  107,  108,  109,  110,  111,  112,
	  113,  114,  118,  119,  120,  121,  122,  123,  124,  125,  126,  127,
	  128,  129,  130,  131,  132,  133,  134,  135,  136,  138,  139,  147,
	  140,  141,  145,  142,  143,  144,  146,  148,  150,  153,  154,  155,
	  156,  157,  158,  159,  160,  161,  162,  163,  179,  164,  164,  165,
	  168,  166,  167,  162,  163,  165,  168,  169,  165,  169,  171,  172,
	  180,  173,  173,  174,  177,  175,  176,  171,  172,  174,  177,  178,
	  174,  178
	};
}

private static final short _warc_trans_targs[] = init__warc_trans_targs_0();


private static byte[] init__warc_trans_actions_0()
{
	return new byte [] {
	    0,    0,    0,    1,    0,    0,    1,   15,    1,    1,   17,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,   19,    1,    0,
	    0,    0,    1,   34,    1,    1,   21,    1,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    1,    1,    1,    1,    0,
	    0,    0,    0,    1,    1,    1,   21,    1,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
	    0,    0,    1,    1,    1,   21,    1,    0,    0,    1,    0,    0,
	    0,    0,    0,    0,    0,    0,    1,    1,    1,   21,    0,    0,
	    0,    0,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
	    1,    1,    1,    1,    1,    1,    1,    1,    1,    0,    0,    0,
	    0,    3,    0,    5,    0,    0,    0,    1,   23,   11,    0,    0,
	    1,    0,    0,   13,   31,    9,   28,   25,    7,    1,    0,    1,
	    0,   11,    0,    0,    1,    0,    0,   13,   31,    9,   28,   25,
	    7,    1
	};
}

private static final byte _warc_trans_actions[] = init__warc_trans_actions_0();


static final int warc_start = 1;
static final int warc_first_final = 179;
static final int warc_error = 0;

static final int warc_en_warc_fields = 170;
static final int warc_en_any_header = 1;


// line 284 "WarcParser.rl"
}