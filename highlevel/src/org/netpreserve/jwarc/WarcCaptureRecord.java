/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.ProtocolVersion;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * A type of WARC record created as part of a web capture event.
 */
public abstract class WarcCaptureRecord extends WarcTargetRecord {
    WarcCaptureRecord(ProtocolVersion version, Headers headers, WarcBody body) {
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
        return headers().all("WARC-Concurrent-To").stream().map(URI::create).collect(toList());
    }

    public abstract static class Builder<R extends WarcCaptureRecord, B extends Builder<R, B>> extends WarcRecord.Builder<R, B> {
        public B concurrentTo(URI recordId) {
            return header("WARC-Concurrent-To", recordId.toString());
        }
    }
}
