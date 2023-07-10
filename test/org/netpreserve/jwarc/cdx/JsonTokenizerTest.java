/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */
package org.netpreserve.jwarc.cdx;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.netpreserve.jwarc.cdx.JsonToken.*;

public class JsonTokenizerTest {
    static List<JsonToken> tokenize(String json) throws IOException, JsonException {
        List<JsonToken> tokens = new ArrayList<>();
        JsonTokenizer parser = new JsonTokenizer(new StringReader(json));
        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) break;
            tokens.add(token);
        }
        return tokens;
    }

    static List<Object> tokenizeValues(String json) throws IOException, JsonException {
        List<Object> values = new ArrayList<>();
        JsonTokenizer parser = new JsonTokenizer(new StringReader(json));
        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) break;
            if (token == STRING) {
                values.add(parser.stringValue());
            } else if (token == NUMBER_INT) {
                values.add(Integer.parseInt(parser.stringValue()));
            } else if (token == NUMBER_FLOAT) {
                values.add(Double.parseDouble(parser.stringValue()));
            } else {
                values.add(token);
            }
        }
        return values;
    }

    @Test
    public void test() throws IOException, JsonException {
        assertEquals(Arrays.asList(START_ARRAY, END_ARRAY), tokenize("[]"));
        assertEquals(Arrays.asList(START_ARRAY, NUMBER_INT, END_ARRAY), tokenize("[5]"));
        assertEquals(Arrays.asList(START_ARRAY, NUMBER_INT, NUMBER_INT, END_ARRAY), tokenize("[5, 6]"));
        assertEquals(Arrays.asList(START_ARRAY, NUMBER_INT, NUMBER_FLOAT, END_ARRAY), tokenize(" [ 5,\t\t6.0 ] "));
        assertEquals(Arrays.asList(START_ARRAY, NUMBER_INT, NUMBER_FLOAT, STRING, END_ARRAY), tokenize("[5,6.0,\"foo\"]"));
        assertEquals(Arrays.asList(START_ARRAY, NUMBER_INT, NUMBER_FLOAT, STRING, TRUE, FALSE, NULL, END_ARRAY), tokenize("[5,6.0,\"foo\",true,false,null]"));
        assertEquals(Arrays.asList(START_OBJECT, FIELD_NAME, NUMBER_INT, END_OBJECT), tokenize("{\"foo\":5}"));
        assertEquals(Arrays.asList(START_OBJECT, FIELD_NAME, NUMBER_INT, FIELD_NAME, NUMBER_FLOAT, END_OBJECT), tokenize("{\"foo\":5,\"bar\":6.0}"));
        assertEquals(Arrays.asList(START_OBJECT, FIELD_NAME, NUMBER_INT, FIELD_NAME, NUMBER_FLOAT, FIELD_NAME, STRING, END_OBJECT), tokenize("{\"foo\":5,\"bar\":6.0,\"baz\":\"q\"}"));
        assertEquals(Arrays.asList(START_OBJECT, FIELD_NAME, START_OBJECT, FIELD_NAME, NUMBER_INT, END_OBJECT, END_OBJECT), tokenize("{\"foo\":{\"bar\":5}}"));
        assertEquals(Arrays.asList(START_OBJECT, FIELD_NAME, START_ARRAY, NUMBER_INT, START_ARRAY, END_ARRAY, NUMBER_FLOAT, END_ARRAY, END_OBJECT), tokenize("{\"foo\":[5,[],6.0]}"));
        assertEquals(singletonList(NUMBER_FLOAT), tokenize("0.0"));
        assertEquals(singletonList(NUMBER_FLOAT), tokenize("1e0"));
        assertEquals(singletonList(NUMBER_FLOAT), tokenize("1e+0"));
        assertEquals(singletonList(NUMBER_FLOAT), tokenize("1e-0"));
        assertEquals(singletonList(NUMBER_FLOAT), tokenize("1.0e0"));
        assertEquals(singletonList(NUMBER_FLOAT), tokenize("1.0e+0"));
        assertEquals(singletonList(NUMBER_FLOAT), tokenize("1.0e-0"));
        assertEquals(Arrays.asList(START_ARRAY, 0.0, -0.0, 1.0, 5, END_ARRAY), tokenizeValues("[0.0, -0.0, 1.0, 5]"));
        assertEquals(singletonList(" \t\r\n\0ሴ\"\\/"), tokenizeValues("\" \\t\\r\\n\\u0000\\u1234\\\"\\\\\\/\""));
    }

}