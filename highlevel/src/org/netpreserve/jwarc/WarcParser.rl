// recompile: ragel -J WarcParser.rl -o WarcParser.java
// diagram:   ragel -Vp WarcParser.rl | dot -TPng | feh -

package org.netpreserve.jwarc;

import javax.annotation.Generated;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.US_ASCII;

%%{

machine warc;

getkey data.get(p);

action push         { push(data.get(p)); }
action push_space   { if (bufPos > 0) push((byte)' '); }
action add_major    { major = major * 10 + data.get(p) - '0'; }
action add_minor    { minor = minor * 10 + data.get(p) - '0'; }
action end_of_text  { endOfText = bufPos; }

action handle_name  {
    name = new String(buf, 0, bufPos, US_ASCII);
    bufPos = 0;
}

action handle_value {
    String value = new String(buf, 0, endOfText, UTF_8);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}

CRLF = "\r\n";

version_major = digit+ $add_major;
version_minor = digit+ $add_minor;
version = "WARC/" version_major "." version_minor CRLF ;

CTL = cntrl | 127;
WS = " " | "\t";
RWS = WS+;
OWS = WS*;
LWS = CRLF RWS;
WORD = (any - CTL - WS)+;
TEXT = WORD (RWS WORD)* %end_of_text;

separators = "(" | ")" | "<" | ">" | "@"
           | "," | ";" | ":" | "\\" | '"'
           | "/" | "[" | "]" | "?" | "="
           | "{" | "}" | " " | "\t";

field_name = ((ascii - CTL - separators)+) $push %handle_name;
field_value_first = OWS (TEXT OWS)? $push;
field_value_folded = LWS (TEXT OWS)? >push_space $push;
field_value = field_value_first (field_value_folded)*;
named_field = field_name ":" field_value CRLF %handle_value;
named_fields = named_field* CRLF;
warc_fields := named_fields;
warc_header := version named_fields @{ fbreak; };

}%%

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
    private Map<String,List<String>> headerMap;

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

        %% write exec;

        position += p - data.position();
        data.position(p);
    }

    public boolean parse(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (true) {
            parse(buffer);
            if (isFinished()) {
                return true;
            }
            if (isError()) throw new ParsingException("invalid WARC record");
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
        return new ProtocolVersion("WARC", major, minor);
    }

    public long position() {
        return position;
    }

    %% write data;
}