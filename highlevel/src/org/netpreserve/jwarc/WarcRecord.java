/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.lowlevel.WarcHeaders;

import java.time.Instant;

public interface WarcRecord extends Message {

    /**
     * The type of WARC record. No identifier scheme is mandated by this specification, but each WARC-Record-ID shall be
     * a legal URI and clearly indicate a documented and registered scheme to which it conforms (e.g. via a URI scheme
     * prefix such as "http:" or "urn:").
     *
     * All records shall have a WARC-Record-ID field.
     */
    default String getType() {
        return getHeaders().get(WarcHeaders.WARC_TYPE);
    }

    /**
     * An identifier assigned to the current record that is globally unique for its period of intended use.
     */
    default String getRecordId() {
        return getHeaders().get(WarcHeaders.WARC_RECORD_ID);
    }

    /**
     * The instant that data capture for record creation began. Multiple records written as part of a single capture
     * event should use the same date, even though the times of their writing will not be exactly synchronized.
     */
    default Instant getDate() {
        return Instant.parse(getHeaders().get(WarcHeaders.WARC_DATE));
    }

    /**
     * An optional parameter indicating the algorithm name and calculated value of a digest applied to the full block
     * of the record.
     */
    default Digest getBlockDigest() {
        String value = getHeaders().get(WarcHeaders.WARC_BLOCK_DIGEST);
        return value == null ? null : new Digest(value);
    }

    /**
     * For practical reasons, writers of a WARC file may place limits on the time or storage allocated to archiving
     * a single resource. As a result, only a truncated portion of the original resource may be available for saving
     * into a WARC record.
     */
    default TruncationReason getTruncated() {
        String value = getHeaders().get(WarcHeaders.WARC_TRUNCATED);
        return value == null ? null : TruncationReason.valueOf(value.toUpperCase());
    }

    /**
     * When present the record ID of the associated {@link WarcInfo} record for this record. Typically, this is set
     * only when the {@link WarcInfo} is not available from context such as after distributing single records into
     * separate WARC files.
     */
    default String getWarcinfoId() {
        return getHeaders().get(WarcHeaders.WARC_WARCINFO_ID);
    }

    /**
     * The current record's relative ordering in a sequence of segmented records.
     *
     * In the first segment of any record that is completed in one or more later {@link WarcContinuation} records, this
     * parameter is mandatory. Its value there is "1". In a {@link WarcContinuation} record, this parameter is also
     * mandatory. Its value is the sequence number of the current segment in the logical whole record, increasing by
     * 1 in each next segment.
     */
    default Long getSegmentNumber() {
        String value = getHeaders().get(WarcHeaders.WARC_SEGMENT_NUMBER);
        return value == null ? null : Long.valueOf(value);
    }

    interface Builder<R extends WarcRecord, B extends Builder<R,B>> extends Message.Builder<R,B> {
        default B setRecordId(String recordId) {
            return setHeader(WarcHeaders.WARC_RECORD_ID, recordId);
        }

        default B setDate(Instant date) {
            return setHeader(WarcHeaders.WARC_DATE, date.toString());
        }

        default B setBlockDigest(String algorithm, String value) {
            return setBlockDigest(new Digest(algorithm, value));
        }

        default B setBlockDigest(Digest digest) {
            return setHeader(WarcHeaders.WARC_BLOCK_DIGEST, digest.toPrefixedBase32());
        }

        default B setTruncated(TruncationReason truncationReason) {
            return setHeader(WarcHeaders.WARC_TRUNCATED, truncationReason.name().toLowerCase());
        }

        default B setWarcinfoId(String warcinfoId) {
            return setHeader(WarcHeaders.WARC_WARCINFO_ID, warcinfoId);
        }

        default B setSegmentNumber(long segmentNumber) {
            return setHeader(WarcHeaders.WARC_SEGMENT_NUMBER, String.valueOf(segmentNumber));
        }
    }
}
