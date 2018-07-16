/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A message consisting of headers and a content block. Forms the basis of protocols and formats like HTTP and WARC.
 */
public abstract class Message {
    private final MessageVersion version;
    private final MessageHeaders headers;
    private final MessageBody body;

    Message(MessageVersion version, MessageHeaders headers, MessageBody body) {
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    /**
     * The named header fields of this message.
     */
    public MessageHeaders headers() {
        return headers;
    }

    /**
     * The content body of this message.
     */
    public MessageBody body() {
        return body;
    }

    /**
     * The version of the network protocol or file format containing this message.
     */
    public MessageVersion version() {
        return version;
    }

    /**
     * The media type of the body.
     * <p>
     * Returns "application/octet-stream" if the Content-Type header is missing.
     */
    public MediaType contentType() {
        return headers.sole("Content-Type").map(MediaType::parse).orElse(MediaType.OCTET_STREAM);
    }

    void serializeHeaderTo(Appendable output) throws IOException {
        output.append(version().toString());
        output.append("\r\n");
        headers.appendTo(output);
        output.append("\r\n");
    }

    Charset headerCharset() {
        return StandardCharsets.UTF_8;
    }

    /**
     * Serializes the message header.
     */
    byte[] serializeHeader() {
        try {
            StringBuilder sb = new StringBuilder();
            serializeHeaderTo(sb);
            return sb.toString().getBytes(headerCharset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static abstract class AbstractBuilder<R extends Message, B extends AbstractBuilder<R, B>> {
        protected Map<String, List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        protected MessageVersion version;

        public AbstractBuilder(MessageVersion defaultVersion) {
            this.version = defaultVersion;
        }

        public abstract R build();

        public B addHeader(String name, String value) {
            headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
            return (B) this;
        }

        public B setHeader(String name, String value) {
            List list = new ArrayList();
            list.add(value);
            headerMap.put(name, list);
            return (B) this;
        }

        public B version(MessageVersion version) {
            this.version = version;
            return (B)this;
        }
    }
}
