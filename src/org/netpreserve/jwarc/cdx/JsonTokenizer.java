/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */
package org.netpreserve.jwarc.cdx;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.netpreserve.jwarc.cdx.JsonToken.*;

class JsonTokenizer {
    private final Reader reader;
    private final StringBuilder buffer = new StringBuilder();
    private final Deque<JsonToken> context = new ArrayDeque<>();
    private final int maxStringLength;
    private final int maxDepth;
    private JsonToken currentToken;
    private int currentCharacter;
    private boolean reconsuming;

    public JsonTokenizer(Reader reader) {
        this(reader, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public JsonTokenizer(Reader reader, int maxDepth, int maxStringLength) {
        this.reader = reader;
        this.maxDepth = maxDepth;
        this.maxStringLength = maxStringLength;
    }

    private int read() throws IOException {
        if (reconsuming) {
            reconsuming = false;
        } else {
            currentCharacter = reader.read();
        }
        return currentCharacter;
    }

    private int readSkippingWhitespace() throws IOException {
        while (true) {
            int c = read();
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            return c;
        }
    }

    public JsonToken nextToken() throws IOException, JsonException {
        if (currentToken == START_OBJECT) {
            currentToken = tokenizeFieldName();
        } else if (currentToken == null || currentToken == START_ARRAY || currentToken == FIELD_NAME) {
            currentToken = tokenizeValue();
        } else {
            currentToken = tokenizeAfterValue();
        }
        return currentToken;
    }

    public String stringValue() {
        return buffer.toString();
    }

    private JsonToken tokenizeFieldName() throws IOException, JsonException {
        switch (readSkippingWhitespace()) {
            case '"':
                consumeString();
                if (readSkippingWhitespace() != ':') throw new JsonException("Expected :");
                return FIELD_NAME;
            case '}':
                if (context.pollLast() != END_OBJECT) throw new JsonException("Unexpected }");
                return END_OBJECT;
            case -1:
                throw new JsonException("Unexpected end of input");
            default:
                throw new JsonException("Unexpected character: " + (char) currentCharacter);
        }
    }

    private JsonToken tokenizeValue() throws IOException, JsonException {
        switch (readSkippingWhitespace()) {
            case -1:
                if (!context.isEmpty()) throw new JsonException("Unexpected end of input");
                return null;
            case '{':
                if (context.size() >= maxDepth) throw new JsonException("Exceeded max depth");
                context.addLast(START_OBJECT);
                return START_OBJECT;
            case '[':
                if (context.size() >= maxDepth) throw new JsonException("Exceeded max depth");
                context.addLast(START_ARRAY);
                return START_ARRAY;
            case '}':
                if (context.pollLast() != START_OBJECT) throw new JsonException("Unexpected }");
                return END_OBJECT;
            case ']':
                if (context.pollLast() != START_ARRAY) throw new JsonException("Unexpected ]");
                return END_ARRAY;
            case 'f':
                return consume("alse", FALSE);
            case 't':
                return consume("rue", TRUE);
            case 'n':
                return consume("ull", NULL);
            case '"':
                return consumeString();
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
            case '.':
                return consumeNumber();
            default:
                throw new JsonException("Unexpected character: " + (char) currentCharacter);
        }
    }

    private JsonToken tokenizeAfterValue() throws IOException, JsonException {
        switch (readSkippingWhitespace()) {
            case -1:
                if (!context.isEmpty()) throw new JsonException("Unexpected end of input");
                return null;
            case ',':
                if (context.isEmpty()) throw new JsonException("Unexpected ,");
                if (context.peekLast() == START_OBJECT) return tokenizeFieldName();
                return tokenizeValue();
            case '}':
                if (context.pollLast() != START_OBJECT) throw new JsonException("Unexpected }");
                return END_OBJECT;
            case ']':
                if (context.pollLast() != START_ARRAY) throw new JsonException("Unexpected ]");
                return END_ARRAY;
            default:
                throw new JsonException("Unexpected character");
        }
    }

    private JsonToken consumeNumber() throws IOException {
        buffer.setLength(0);
        buffer.append((char) currentCharacter);
        JsonToken result = NUMBER_INT;
        while (true) {
            int c = read();
            switch (c) {
                case -1:
                    return result;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '+':
                case '-':
                    buffer.append((char) c);
                    break;
                case '.':
                case 'e':
                case 'E':
                    result = NUMBER_FLOAT;
                    buffer.append((char) c);
                    break;
                default:
                    reconsuming = true;
                    return result;
            }
        }
    }

    private JsonToken consumeString() throws IOException, JsonException {
        buffer.setLength(0);
        while (true) {
            int c = read();
            switch (c) {
                case -1:
                    throw new JsonException("Unterminated string");
                case '"':
                    return STRING;
                case '\\':
                    int escapedChar;
                    switch (read()) {
                        case -1:
                            throw new JsonException("Unterminated string");
                        case 'b':
                            escapedChar = '\b';
                            break;
                        case 'f':
                            escapedChar = '\f';
                            break;
                        case 'n':
                            escapedChar = '\n';
                            break;
                        case 'r':
                            escapedChar = '\r';
                            break;
                        case 't':
                            escapedChar = '\t';
                            break;
                        case 'u':
                            escapedChar = 0;
                            for (int i = 0; i < 4; i++) {
                                c = read();
                                if (c == -1) throw new JsonException("Unterminated string");
                                int digit = Character.digit(c, 16);
                                if (digit == -1) throw new JsonException("Expected hex digit");
                                escapedChar = escapedChar * 16 + digit;
                            }
                            break;
                        default:
                            throw new JsonException("Invalid escape sequence: \\" + (char) currentCharacter);
                    }
                    if (buffer.length() < maxStringLength) buffer.append((char) escapedChar);
                    break;
                default:
                    if (buffer.length() < maxStringLength) buffer.append((char) c);
                    break;
            }
        }
    }

    private JsonToken consume(String expected, JsonToken token) throws IOException, JsonException {
        for (int i = 0; i < expected.length(); i++) {
            if (read() != expected.charAt(i)) throw new JsonException("Expected " + expected);
        }
        return token;
    }

    public JsonToken currentToken() {
        return currentToken;
    }
}
