/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * A type of WARC record created as part of a web capture event.
 */
public abstract class WarcCaptureRecord extends WarcTargetRecord {
    WarcCaptureRecord(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    /**
     * The IP address of the server involved in the capture event this record belongs to.
     */
    public Optional<InetAddress> ipAddress() {
        return headers().sole("WARC-IP-Address").map(InetAddresses::forString);
    }

    /**
     * The IDs of other records created during the same capture event as this one.
     */
    public List<URI> concurrentTo() {
        return headers().all("WARC-Concurrent-To").stream().map(WarcRecord::parseRecordID).collect(toList());
    }

    public abstract static class AbstractBuilder<R extends WarcCaptureRecord, B extends AbstractBuilder<R, B>> extends WarcTargetRecord.Builder<R, B> {
        protected AbstractBuilder(String type) {
            super(type);
        }

        public B body(MediaType type, Message message) {
            ByteBuffer header = ByteBuffer.wrap(message.serializeHeader());
            ReadableByteChannel channel = IOUtils.prefixChannel(header, message.body());
            return body(type, channel, message.body().size() + header.remaining());
        }

        public B concurrentTo(URI recordId) {
            return addHeader("WARC-Concurrent-To", recordId.toString());
        }
    }
}
