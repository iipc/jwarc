// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -
%%{

machine http;

getkey (data.get(p) & 0xff);

action push        { push(data.get(p)); }
action push_space  { if (bufPos > 0) push((byte)' '); }
action add_major   { major = major * 10 + data.get(p) - '0'; }
action add_minor   { minor = minor * 10 + data.get(p) - '0'; }
action add_status  { status = status * 10 + data.get(p) - '0'; }
action end_of_text { endOfText = bufPos; }
action handle_method  { method = new String(buf, 0, bufPos, US_ASCII); bufPos = 0; }
action handle_reason  { reason = new String(buf, 0, bufPos, ISO_8859_1); bufPos = 0; }
action handle_target  { target = new String(buf, 0, bufPos, ISO_8859_1); bufPos = 0; }
action finish { finished = true; }

action fold {
    if (bufPos > 0) {
        bufPos = endOfText;
        push((byte)' ');
    }
}

action handle_name  {
    name = new String(buf, 0, bufPos, US_ASCII).trim();
    bufPos = 0;
}

action handle_value {
    String value = new String(buf, 0, endOfText, ISO_8859_1);
    headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    bufPos = 0;
    endOfText = 0;
}

CRLF = "\r\n";
CTL = cntrl | 127;
WS = " " | "\t";
RWS = WS+;
OWS = WS*;
LWS = CRLF RWS;
VCHAR = 0x21..0x7E;
tchar = "!" | "#" | "$" | "%" | "&" | "'" | "*" | "+" | "-" | "." |
        "^" | "_" | "`" | "|" | "~" | digit | alpha;
token = tchar+;
obs_text = 0x80..0xff;
url_byte =  alpha | digit | "!" | "$" | "&" | "'" | "(" | ")" |
            "*" | "+" | "," | "-" | "." | "/" | ":" | ";" |
            "=" | "?" | "@" | "_" | "~" | "%" | 0x80..0xff;

version_major = digit $add_major;
version_minor = digit $add_minor;
http_version = "HTTP/" version_major "." version_minor;

method = token $push %handle_method;
request_target = url_byte+ $push %handle_target;
request_line = method " " request_target " " http_version CRLF;

status_code = digit {3} $add_status;
reason_phrase = ("\t" | " " | VCHAR | obs_text)* $push %handle_reason;
status_line = http_version " " status_code " " reason_phrase CRLF;

field_vchar = VCHAR | obs_text;
WORD = field_vchar+;
TEXT = WORD (RWS WORD)* %end_of_text;

field_name = token $push %handle_name;
field_value_first = OWS (TEXT OWS)? $push;
field_value_folded = LWS (TEXT OWS)? >fold $push;
field_value = field_value_first (field_value_folded)*;
named_field = field_name ":" field_value CRLF %handle_value;
named_fields = named_field* CRLF;

http_request := request_line named_fields @finish;
http_response := status_line named_fields @finish;

#
# Variations for lenient parsing:
#

CRLF_lenient = "\r"* "\n";
LWS_lenient = CRLF_lenient RWS;
TEXT_lenient = ((any - '\n' - WS) (any - '\n')*)? (any - '\n' - WS - '\r') %end_of_text;

request_target_lenient = (any - ' ' - '\n' - '\r')+ $push %handle_target;
request_line_lenient = method " "+ request_target_lenient (" "+ http_version)? " "* CRLF_lenient;

status_line_lenient = http_version " "+ status_code (" " reason_phrase)? CRLF_lenient;

field_name_lenient = ((any - '\r' - '\n' - ' ' - '\t' - ':') (any - '\r' - '\n' - ':')*) $push;
field_value_first_lenient = OWS (TEXT_lenient OWS)? $push;
field_value_folded_lenient = LWS (TEXT_lenient OWS)? >fold $push;
field_value_lenient = field_value_first_lenient (field_value_folded_lenient)*;
named_field_lenient = (field_name_lenient OWS)? ":" >handle_name field_value_lenient CRLF_lenient %handle_value;
named_fields_lenient = named_field_lenient* CRLF_lenient;

http_request_lenient := request_line_lenient named_fields_lenient @finish;
http_response_lenient := status_line_lenient named_fields_lenient @finish;

}%%

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
        %% write init;
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

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) {
        int p = data.position();
        int pe = data.limit();

        %% write exec;

        position += p - data.position();
        data.position(p);
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
            if (n < 0) throw new EOFException("state=" + cs);
            buffer.flip();
        }
    }

    private void push(byte b) {
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    %% write data;
}