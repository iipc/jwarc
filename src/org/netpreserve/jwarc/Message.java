/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A message consisting of headers and a content block (body). Forms the basis of protocols and formats like HTTP and
 * WARC.
 */
public abstract class Message {
    private final MessageVersion version;
    private final MessageHeaders headers;
    private final MessageBody body;
    byte[] serializedHeader;

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
        return headers.first("Content-Type").map(MediaType::parse).orElse(MediaType.OCTET_STREAM);
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
    public byte[] serializeHeader() {
        if (serializedHeader == null) {
            try {
                StringBuilder sb = new StringBuilder();
                serializeHeaderTo(sb);
                serializedHeader = sb.toString().getBytes(headerCharset());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return serializedHeader;
    }

    @SuppressWarnings("unchecked")
    public static abstract class AbstractBuilder<R extends Message, B extends AbstractBuilder<R, B>> {
        protected Map<String, List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        protected MessageVersion version;
        protected ReadableByteChannel bodyChannel;

        public AbstractBuilder(MessageVersion defaultVersion) {
            this.version = defaultVersion;
        }

        /**
         * Finishes building.
         */
        public abstract R build();

        /**
         * Adds a header field. Existing headers with the same name are not removed.
         */
        public B addHeader(String name, String value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            headerMap.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
            return (B) this;
        }

        /**
         * Appends header fields. Existing headers with the same name are not removed.
         */
        public B addHeaders(Map<String, List<String>> headers) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String value : entry.getValue()) {
                    addHeader(entry.getKey(), value);
                }
            }
            return (B) this;
        }

        /**
         * Sets a header field. Removes any existing headers with the same name. If <code>value</code> is null then
         * simply removes all existing headers with the given name.
         */
        public B setHeader(String name, String value) {
            Objects.requireNonNull(name);
            if (value == null) {
                headerMap.remove(name);
            } else {
                List<String> list = new ArrayList<>();
                list.add(value);
                headerMap.put(name, list);
            }
            return (B) this;
        }

        /**
         * Sets the protocol version of this message or record.
         */
        public B version(MessageVersion version) {
            this.version = version;
            return (B)this;
        }

        /**
         * Sets the message body. The Content-Length header will be set to the length of <code>contentBytes</code>.
         * The Content-Type header will be set to <code>contentType</code> unless it is null.
         */
        public B body(MediaType contentType, byte[] contentBytes) {
            return body(contentType, Channels.newChannel(new ByteArrayInputStream(contentBytes)), contentBytes.length);
        }

        /**
         * Sets the message body. The Content-Length header will be set to <code>length</code>.
         * The Content-Type header will be set to <code>contentType</code> unless it is null.
         */
        public B body(MediaType contentType, ReadableByteChannel channel, long length) {
            if (contentType != null) {
                setHeader("Content-Type", contentType.toString());
            }
            setHeader("Content-Length", Long.toString(length));
            this.bodyChannel = channel;
            return (B) this;
        }

        MessageBody makeBody() {
            long contentLength = 0;
            List<String> values = headerMap.get("Content-Length");
            if (values != null && !values.isEmpty()) {
                contentLength = Long.parseLong(values.get(0));
            }
            return LengthedBody.create(bodyChannel, ByteBuffer.allocate(0), contentLength);
        }
    }
}
