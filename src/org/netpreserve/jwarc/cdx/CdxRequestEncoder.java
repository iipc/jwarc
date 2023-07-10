/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */
package org.netpreserve.jwarc.cdx;

import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.IOUtils;
import org.netpreserve.jwarc.MediaType;

import java.io.*;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

// http://iipc.github.io/warc-specifications/guidelines/cdx-non-get-requests/
public class CdxRequestEncoder {
    private static final int QUERY_STRING_LIMIT = 4096;
    private static final int BUFFER_SIZE = 64 * 1024;

    public static String encode(HttpRequest httpRequest) throws IOException {
        if (httpRequest.method().equals("GET")) return null;
        StringBuilder out = new StringBuilder();
        out.append("__wb_method=");
        out.append(httpRequest.method());
        int maxLength = out.length() + 1 + QUERY_STRING_LIMIT;
        MediaType baseContentType = httpRequest.contentType().base();
        InputStream stream = new BufferedInputStream(httpRequest.body().stream(), BUFFER_SIZE);
        if (baseContentType.equals(MediaType.WWW_FORM_URLENCODED)) {
            encodeFormBody(stream, out);
        } else if (baseContentType.equals(MediaType.JSON)) {
            encodeJsonBody(stream, out, maxLength, false);
        } else if (baseContentType.equals(MediaType.PLAIN_TEXT)) {
            encodeJsonBody(stream, out, maxLength, true);
        } else {
            encodeBinaryBody(stream, out);
        }
        return out.substring(0, Math.min(out.length(), maxLength));
    }

    static void encodeBinaryBody(InputStream stream, StringBuilder out) throws IOException {
        byte[] body = IOUtils.readNBytes(stream, QUERY_STRING_LIMIT);
        out.append("&__wb_post_data=");
        out.append(Base64.getEncoder().encodeToString(body));
    }

    private static void encodeFormBody(InputStream stream, StringBuilder out) throws IOException {
        stream.mark(BUFFER_SIZE);
        try {
            byte[] body = IOUtils.readNBytes(stream, QUERY_STRING_LIMIT);
            String decodedBody = String.valueOf(UTF_8.newDecoder().decode(ByteBuffer.wrap(body)));
            out.append('&');
            out.append(URLDecoder.decode(decodedBody, "utf-8"));
        } catch (MalformedInputException e) {
            stream.reset();
            encodeBinaryBody(stream, out);
        }
    }

    private static void encodeJsonBody(InputStream stream, StringBuilder output, int maxLength, boolean binaryFallback) throws IOException {
        stream.mark(BUFFER_SIZE);
        JsonTokenizer tokenizer = new JsonTokenizer(new BufferedReader(new InputStreamReader(stream, UTF_8)),
                QUERY_STRING_LIMIT, QUERY_STRING_LIMIT);
        Map<String,Long> nameCounts = new HashMap<>();
        Deque<String> nameStack = new ArrayDeque<>();
        String name = null;
        try {
            while (tokenizer.nextToken() != null && output.length() < maxLength) {
                switch (tokenizer.currentToken()) {
                    case FIELD_NAME:
                        name = tokenizer.stringValue();
                        break;
                    case FALSE:
                    case TRUE:
                    case NUMBER_FLOAT:
                    case STRING:
                    case NUMBER_INT:
                    case NULL:
                        if (name != null) {
                            long serial = nameCounts.compute(name, (key, value) -> value == null ? 1 : value + 1);
                            String key = name;
                            if (serial > 1) {
                                key += "." + serial + "_";
                            }
                            output.append('&');
                            output.append(percentPlusEncode(key));
                            output.append('=');
                            String encodedValue;
                            switch (tokenizer.currentToken()) {
                                case NULL:
                                    encodedValue = "None"; // using Python names for pywb compatibility
                                    break;
                                case FALSE:
                                    encodedValue = "False";
                                    break;
                                case TRUE:
                                    encodedValue = "True";
                                    break;
                                case NUMBER_INT:
                                    encodedValue = String.valueOf(Long.parseLong(tokenizer.stringValue()));
                                    break;
                                case NUMBER_FLOAT:
                                    encodedValue = String.valueOf(Double.parseDouble(tokenizer.stringValue()));
                                    break;
                                default:
                                    encodedValue = percentPlusEncode(tokenizer.stringValue());
                            }
                            output.append(encodedValue);
                        }
                        break;
                    case START_OBJECT:
                        if (name != null) {
                            nameStack.push(name);
                        }
                        break;
                    case END_OBJECT:
                        name = nameStack.isEmpty() ? null : nameStack.pop();
                        break;
                    case START_ARRAY:
                    case END_ARRAY:
                        break;
                    default:
                        throw new IllegalStateException("Unexpected: " + tokenizer.currentToken());
                }
            }
        } catch (JsonException | NumberFormatException e) {
            if (binaryFallback) {
                try {
                    stream.reset();
                    encodeBinaryBody(stream, output);
                } catch (IOException e2) {
                    // give up
                }
            }
        }
    }

    private static final BitSet percentPlusUnreserved = new BitSet();
    static {
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~".chars()
                .forEach(percentPlusUnreserved::set);
    }

    public static String percentPlusEncode(String string) {
        StringBuilder output = new StringBuilder();
        Formatter formatter = new Formatter(output);
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        for (byte rawByte : bytes) {
            int b = rawByte & 0xff;
            if (percentPlusUnreserved.get(b)) {
                output.append((char) b);
            } else if (b == ' ') {
                output.append('+');
            } else {
                output.append('%');
                formatter.format("%02X", b);
            }
        }
        return output.toString();
    }
}
