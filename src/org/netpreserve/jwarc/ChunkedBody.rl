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

    %%{
        machine chunked;

        getkey buffer.get(p);
        action add_length { tmp = tmp * 16 + Character.digit(buffer.get(p), 16); }
        action end_header { if (tmp != 0) { chunkLength = tmp; tmp = 0; fbreak; } }
        action end_final  { chunkLength = 0; }

        hexdigit = "0".."9" | "a".."f" | "A".."F";
        tchar = "!" | "#" | "$" | "%" | "&" | "'" | "*" | "+" | "-" | "." |
                "^" | "_" | "`" | "|" | "~" | digit | alpha;
        qdtext = "\t" | " " | "!" | "#".."[" | "]".."~" | 0x80..0xFF;
        token = tchar+;
        quoted_pair = "\\" (0x00..0x09 | 0x0B..0x0C | 0x0E..0x7F);
        quoted_string = '"' (qdtext | quoted_pair)* '"';

        obs_text = 0x80..0xff;
        CRLF = "\r\n";
        CTL = cntrl | 127;
        WS = " " | "\t";
        RWS = WS+;
        OWS = WS*;
        LWS = CRLF RWS;
        VCHAR = 0x21..0x7E;
        field_vchar = VCHAR | obs_text;
        WORD = field_vchar+;
        TEXT = WORD (RWS WORD)*;


        field_name = token;
        field_value_first = OWS (TEXT OWS)?;
        field_value_folded = LWS (TEXT OWS)?;
        field_value = field_value_first (field_value_folded)*;
        named_field = field_name ":" field_value CRLF;
        named_fields = named_field* CRLF;

        chunk_value = token | quoted_string;
        chunk_ext = ';' token '=' chunk_value;
        chunk_length = hexdigit+ $add_length;
        chunk_header = chunk_length chunk_ext* "\r\n" @end_header;
        chunk = chunk_header CRLF;
        last_chunk = "0\r\n";
        chunks := chunk* last_chunk named_fields @end_final;
    }%%

    private int cs = chunked_start;
    private long tmp;

    private void parse() throws ParsingException {
        int p = buffer.position();
        int pe = buffer.limit();
        %% write exec;
        buffer.position(p);
        if (cs == chunked_error) {
            throw new ParsingException("chunked encoding at position " + p + ": "
                    + getErrorContext(buffer, (int) p, 40));
        }
    }

    %% write data;
}
