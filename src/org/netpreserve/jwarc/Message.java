/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

/**
 * A message consisting of headers and a content block. Forms the basis of protocols and formats like HTTP and WARC.
 */
public abstract class Message {
    private final ProtocolVersion version;
    private final Headers headers;
    private final BodyChannel body;

    Message(ProtocolVersion version, Headers headers, BodyChannel body) {
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    /**
     * The named header fields of this message.
     */
    public Headers headers() {
        return headers;
    }

    /**
     * The content body of this message.
     */
    public BodyChannel body() {
        return body;
    }

    /**
     * The version of the network protocol or file format containing this message.
     */
    public ProtocolVersion version() {
        return version;
    }

    /**
     * The content type of the body.
     * <p>
     * Returns "application/octet-stream" if the Content-Type header is missing.
     */
    public String contentType() {
        return headers.sole("Content-Type").orElse("application/octet-stream");
    }


    public static abstract class Builder<R extends Message, B extends Builder<R, B>> {
        public abstract R build();

        public abstract B addHeader(String name, String value);

        public abstract B setHeader(String name, String value);
    }
}
