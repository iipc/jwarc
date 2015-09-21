%%{

machine warc;

action push { buf.write(data[p]); }

CRLF = "\r\n";

version_major = digit+ ${ majorVersion = majorVersion * 10 + data[p] - '0'; };
version_minor = digit+ ${ minorVersion = minorVersion * 10 + data[p] - '0'; };
version = "WARC/" version_major "." version_minor CRLF;

CTL = cntrl | 127;
TEXT = (any - CTL - " ");
SPTEXT = (any - CTL) | " " | "\t";


separators = "(" | ")" | "<" | ">" | "@"
           | "," | ";" | ":" | "\\" | '"'
           | "/" | "[" | "]" | "?" | "="
           | "{" | "}" | " " | "\t";

field_name = ((ascii - CTL - separators)+) $push %{ fieldName = slice(); };
field_content_first = (" " | "\t")* (TEXT SPTEXT*)? $push;
field_content_folded = CRLF (" " | "\t")+ (TEXT SPTEXT*)? >{ buf.write('\n'); } $push;
field_content = field_content_first (field_content_folded)*;
named_field = field_name ":" field_content CRLF %{ fields.put(fieldName, slice()); fieldName = null; };
warc_fields = named_field* CRLF;

header := version warc_fields @{ fbreak; };
#warc_record = header CRLF block CRLF CRLF;

}%%

package jwarc;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;

class WarcParser {
    final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    Map<String,String> fields;
    int cs;
    String fieldName;

    int minorVersion;
    int majorVersion;

    WarcParser() {
        reset();
    }

    void reset() {
        %% write init;
        buf.reset();
        majorVersion = 0;
        minorVersion = 0;
        fields = new LinkedTreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    boolean isFinished() {
        return cs == warc_first_final;
    }

    boolean isError() {
        return cs == warc_error;
    }

	void feed(ByteBuffer buffer) {
		int consumed = feed(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
		buffer.position(buffer.position() + consumed);
	}

    int feed(byte[] data) {
        return feed(data, 0, data.length);
    }

    int feed(byte[] data, int start, int length) {
        int p = start, pe = p + length, eof = pe;

        %% write exec;

        return p - start;
    }

    private String slice() {
        try {
            return buf.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
            buf.reset();
        }
    }

    %% write data;
}