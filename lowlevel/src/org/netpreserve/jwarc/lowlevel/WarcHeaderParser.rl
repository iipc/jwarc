// recompile: ragel -J WarcHeaderParser.rl -o WarcHeaderParser.java
// diagram:   ragel -Vp WarcHeaderParser.rl | dot -TPng | feh -
%%{

machine warc;

getkey data.get(p);

action push         { push(data.get(p)); }
action push_space   { push((byte)' '); }
action add_digit    { number = number * 10 + data.get(p) - '0'; }
action end_of_text  { endOfText = bufPos; }
action handle_major { handler.majorVersion(number); number = 0; }
action handle_minor { handler.minorVersion(number); number = 0; }
action handle_field { handler.field(new HeaderField(buf, bufPos)); bufPos = 0; }
action handle_value { handler.value(new String(buf, 0, endOfText, UTF_8)); bufPos = 0; endOfText = 0; }

CRLF = "\r\n";

version_major = digit+ $add_digit %handle_major;
version_minor = digit+ $add_digit %handle_minor;
version = "WARC/" version_major "." version_minor CRLF ;

CTL = cntrl | 127;
WS = " " | "\t";
WORD = (any - CTL - WS)+;
RWS = WS+;
OWS = WS*;
LWS = CRLF RWS;
TEXT = WORD (RWS WORD)* %end_of_text;


separators = "(" | ")" | "<" | ">" | "@"
           | "," | ";" | ":" | "\\" | '"'
           | "/" | "[" | "]" | "?" | "="
           | "{" | "}" | " " | "\t";

field_name = ((ascii - CTL - separators)+) $push %handle_field;
field_content_first = OWS (TEXT OWS)? $push;
field_content_folded = LWS (TEXT OWS)? >push_space $push;
field_content = field_content_first (field_content_folded)*;
named_field = field_name ":" field_content CRLF %handle_value;
warc_fields = named_field* CRLF;

header := version warc_fields @{ fbreak; };


}%%

package org.netpreserve.jwarc.lowlevel;

import java.nio.ByteBuffer;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class WarcHeaderParser {
    private final WarcHeaderHandler handler;
    private int cs;
    private byte[] buf = new byte[256];
    private int bufPos = 0;
    private int endOfText;
    private int number;

    public WarcHeaderParser(WarcHeaderHandler handler) {
        this.handler = handler;
        reset();
    }

    public void reset() {
        %% write init;
        bufPos = 0;
        if (buf.length > 8192) {
            buf = new byte[256]; // if our buffer grew really big release it
        }
        number = 0;
        endOfText = 0;
    }

    public boolean isFinished() {
        return cs == warc_first_final;
    }

    public boolean isError() {
        return cs == warc_error;
    }

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) {
        int p = data.position();
        int pe = data.limit();

        %% write exec;

        data.position(p);
    }

    private void push(byte b) {
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    %% write data;
}