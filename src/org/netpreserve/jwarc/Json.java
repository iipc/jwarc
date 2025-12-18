/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2025 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class Json {
    static Object read(InputStream stream) throws IOException {
        return read(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
    }

    static Object read(Reader reader) throws IOException {
        return new Parser(reader).value();
    }

    private static class Parser {
        private final Reader reader;
        private int peek = -2;

        Parser(Reader reader) {
            this.reader = reader;
        }

        private int peek() throws IOException {
            if (peek == -2) peek = reader.read();
            return peek;
        }

        private int next() throws IOException {
            int c = peek();
            peek = -2;
            return c;
        }

        private int look() throws IOException {
            int c = peek();
            while (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                c = reader.read();
            }
            peek = c;
            return c;
        }

        private void consume(int c) throws IOException {
            if (next() != c) throw new IOException("Expected '" + (char) c + "'");
        }

        Object value() throws IOException {
            int c = look();
            if (c == '"') return string();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == 't') return literal("true", true);
            if (c == 'f') return literal("false", false);
            if (c == 'n') return literal("null", null);
            if (c == '-' || (c >= '0' && c <= '9')) return number();
            if (c == -1) throw new EOFException();
            throw new IOException("Unexpected character");
        }

        private Object number() throws IOException {
            StringBuilder buffer = new StringBuilder();
            boolean dbl = false;
            while (true) {
                int c = peek();
                if (c == 'e' || c == 'E' || c == '.') {
                    dbl = true;
                } else if ((c < '0' || c > '9') && c != '-' && c != '+') {
                    try {
                        if (dbl) return Double.parseDouble(buffer.toString());
                        return Long.parseLong(buffer.toString());
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid number: " + buffer);
                    }
                }
                buffer.append((char) next());
            }
        }

        private Object literal(String s, Boolean value) throws IOException {
            for (int i = 0; i < s.length(); i++) {
                if (next() != s.charAt(i)) throw new IOException("Expected '" + s + "'");
            }
            return value;
        }

        private Object array() throws IOException {
            consume('[');
            Collection<Object> list = new ArrayList<>();
            if (look() != ']') {
                while (true) {
                    list.add(value());
                    if (look() == ']') break;
                    consume(',');
                }
            }
            consume(']');
            return list;
        }

        private Object object() throws IOException {
            consume('{');
            Map<String, Object> map = new LinkedHashMap<>();
            if (look() != '}') {
                while (true) {
                    String key = string();
                    consume(':');
                    map.put(key, value());
                    if (look() == '}') break;
                    consume(',');
                    look();
                }
            }
            consume('}');
            return map;
        }

        private String string() throws IOException {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = next();
                if (c < 0) throw new EOFException("Unterminated JSON string");
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append((char) c);
                    continue;
                }
                c = next();
                if (c == '"' || c == '\\' || c == '/') sb.append((char) c);
                else if (c == 'b') sb.append('\b');
                else if (c == 'f') sb.append('\f');
                else if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else if (c == 'u') {
                    int x = 0;
                    for (int i = 0; i < 4; i++) {
                        c = next();
                        int digit = Character.digit(c, 16);
                        if (digit < 0) throw new IOException("Invalid hex digit in unicode escape: " + (char) c);
                        x = (x << 4) + digit;
                    }
                    sb.append((char) x);
                } else {
                    throw new IOException("Invalid escape character: \\" + (char) c);
                }
            }
        }
    }

    static void write(OutputStream out, Object value) throws IOException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FilterOutputStream(out) {
            public void close() {
            }
        }, StandardCharsets.UTF_8))) {
            write(writer, value);
        }
    }

    static void write(Appendable out, Object value) throws IOException {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Boolean) {
            out.append(value.toString());
        } else if (value instanceof String) {
            out.append('"');
            for (int i = 0; i < ((String) value).length(); i++) {
                char c = ((String) value).charAt(i);
                if (c == '"') out.append("\\\"");
                else if (c == '\\') out.append("\\\\");
                else if (c == '\b') out.append("\\b");
                else if (c == '\f') out.append("\\f");
                else if (c == '\n') out.append("\\n");
                else if (c == '\r') out.append("\\r");
                else if (c == '\t') out.append("\\t");
                else if (c <= 0x1f) {
                    out.append("\\u00");
                    out.append(Character.forDigit((c & 0xf0) >>> 4, 16));
                    out.append(Character.forDigit(c & 0xf, 16));
                } else {
                    out.append(c);
                }
            }
            out.append('"');
        } else if (value instanceof Number) {
            out.append(value.toString());
        } else if (value instanceof Map) {
            out.append('{');
            Map<?, ?> map = (Map<?, ?>) value;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                write(out, entry.getKey());
                out.append(':');
                write(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof Collection) {
            out.append('[');
            Collection<?> coll = (Collection<?>) value;
            boolean first = true;
            for (Object o : coll) {
                if (!first) out.append(',');
                first = false;
                write(out, o);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("unsupported JSON type: " + value.getClass());
        }
    }
}
