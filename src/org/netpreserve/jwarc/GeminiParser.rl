// recompile: ragel -J GeminiParser.rl -o GeminiParser.java
// diagram:   ragel -Vp GeminiParser.rl | dot -Tpng | feh -
// spec: https://gemini.circumlunar.space/docs/specification.gmi
%%{

machine gemini;

getkey (data.get(p) & 0xff);

action push           { push(data.get(p)); }
action add_status     { status = status * 10 + data.get(p) - '0'; }
action handle_meta    { meta = new String(buf, 0, bufPos, UTF_8); bufPos = 0; }
action handle_url     { url = new String(buf, 0, bufPos, UTF_8); bufPos = 0; }
action finish         { finished = true; fbreak; }

CRLF = "\r\n";

# spec doesn't mention any disallowed characters
# but we assume \r and \n
utf8_string = (any - '\r' - '\n')*;

url = utf8_string $push %handle_url;
gemini_request := url CRLF @finish;

meta = utf8_string $push %handle_meta;
status = digit {2} $add_status;
gemini_response := status " " meta CRLF @finish;

}%%

package org.netpreserve.jwarc;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GeminiParser extends MessageParser {
    private int initialState;
    private int cs;
    private long position;
    private boolean finished;
    private byte[] buf = new byte[256];
    private int bufPos = 0;
    private int status;
    private String meta;
    private String url;

	public GeminiParser() {
        reset();
    }

    public void reset() {
        %% write init;
        bufPos = 0;
        if (buf.length > 8192) {
            buf = new byte[256]; // if our buffer grew really big release it
        }
        status = 0;
        meta = null;
        url = null;
        position = 0;
        finished = false;
        cs = initialState;
    }

    public int status() {
        return status;
    }

    public String meta() {
        return meta;
    }

    public String url() {
        return url;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isError() {
        return cs == gemini_error;
    }

    /**
     * Configures the parser to read a gemini request while rejecting deviations from the standard.
     */
    public void strictRequest() {
        cs = gemini_en_gemini_request;
        initialState = cs;
    }

    /**
     * Configures the parser to read a gemini response while rejecting deviations from the standard.
     */
    public void strictResponse() {
        cs = gemini_en_gemini_response;
        initialState = cs;
    }

    /**
     * Runs the parser on a buffer of data. Passing null as the buffer indicates the end of input.
     */
    @SuppressWarnings({"UnusedAssignment", "ConstantConditions", "ConditionalBreakInInfiniteLoop"})
    public void parse(ByteBuffer data) throws ParsingException {
        int p;
        int pe;
        int eof;

        if (data == null) {
            p = 0;
            pe = 0;
            eof = 0;
        } else {
            p = data.position();
            pe = data.limit();
            eof = -1;
        }

        %% write exec;

        if (data != null) {
            position += p - data.position();
            data.position(p);
        }
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
                throw new ParsingException("invalid gemini message at byte position " + position + ": "
                        + getErrorContext(buffer.duplicate(), (int) (buffOffset + position), 40));
            }
            buffer.compact();
            int n = channel.read(buffer);
            buffer.flip();
            if (n < 0) {
                parse(null);
                break;
            }
        }
    }

    private void push(byte b) throws ParsingException {
        if (bufPos >= 1024) throw new ParsingException("gemini header field longer than 1024 bytes");
        if (bufPos >= buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[bufPos++] = b;
    }

    %% write data;
}