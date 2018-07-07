// recompile: ragel -J HttpParser.rl -o HttpParser.java
// diagram:   ragel -Vp HttpParser.rl | dot -Tpng | feh -
%%{

machine http;

getkey data.get(p);

action push        { push(data.get(p)); }
action push_space  { if (bufPos > 0) push((byte)' '); }
action add_major   { major = major * 10 + data.get(p) - '0'; }
action add_minor   { minor = minor * 10 + data.get(p) - '0'; }
action add_status  { status = status * 10 + data.get(p) - '0'; }
action end_of_text { endOfText = bufPos; }
action handle_version { handler.version(major, minor); }
action handle_name    { handler.name(new String(buf, 0, bufPos, US_ASCII)); bufPos = 0; }
action handle_method  { handler.method(new String(buf, 0, bufPos, US_ASCII)); bufPos = 0; }
action handle_reason  { handler.reason(new String(buf, 0, bufPos, ISO_8859_1)); bufPos = 0; }
action handle_status  { handler.status(status); }
action handle_target  { handler.target(new String(buf, 0, bufPos, ISO_8859_1)); bufPos = 0; }
action handle_value   { handler.value(new String(buf, 0, endOfText, ISO_8859_1)); bufPos = 0; endOfText = 0; }
action finish { finished = true; }

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
url_byte =  alpha | digit | "!" | "$" | "&" | "'" | "(" | "(" |
            "*" | "+" | "," | "-" | "." | "/" | ":" | ";" |
            "=" | "?" | "@" | "_" | "~" | 0x80..0xff;

version_major = digit $add_major;
version_minor = digit $add_minor %handle_version;
http_version = "HTTP/" version_major "." version_minor;

method = token $push %handle_method;
request_target = url_byte+ $push %handle_target;
request_line = method " " request_target " " http_version CRLF;

status_code = digit {3} $add_status %handle_status;
reason_phrase = ("\t" | " " | VCHAR | obs_text)* $push %handle_reason;
status_line = http_version " " status_code " " reason_phrase CRLF;

field_vchar = VCHAR | obs_text;
WORD = field_vchar+;
TEXT = WORD (RWS WORD)* %end_of_text;

field_name = token $push %handle_name;
field_value_first = OWS (TEXT OWS)? $push;
field_value_folded = LWS (TEXT OWS)? >push_space $push;
field_value = field_value_first (field_value_folded)*;
named_field = field_name ":" field_value CRLF %handle_value;
named_fields = named_field* CRLF;

http_request := request_line named_fields @finish;
http_response := status_line named_fields @finish;

# start_line = request_line | status_line;
# http_message := start_line named_fields @{ fbreak; };

}%%

package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class HttpParser {
    private final Handler handler;
    private int cs;
    private long position;
    private boolean finished;
    private byte[] buf = new byte[256];
    private int bufPos = 0;
    private int endOfText;
    private int major;
    private int minor;
    private int status;

	public interface Handler {
		void version(int major, int minor);
		void name(String name);
		void value(String value);
		void method(String method);
		void reason(String reason);
		void status(int status);
		void target(String target);
	}

	public HttpParser(Handler handler) {
        this.handler = handler;
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
        endOfText = 0;
        position = 0;
        finished = false;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isError() {
        return cs == http_error;
    }

    public void requestOnly() {
        cs = http_en_http_request;
    }

    public void responseOnly() {
        cs = http_en_http_response;
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
        while (true) {
            parse(buffer);
            if (isFinished()) break;
            if (isError()) throw new ParsingException("invalid HTTP message at byte position " + position);
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