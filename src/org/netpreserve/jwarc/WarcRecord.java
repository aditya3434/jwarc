/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;

public class WarcRecord extends Message {
    WarcRecord(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    static URI parseRecordID(String uri) {
        if (uri.startsWith("<") && uri.endsWith(">")) {
            uri = uri.substring(1, uri.length() - 1);
        }
        return URI.create(uri);
    }

    private static String formatId(UUID recordId) {
        return "<urn:uuid:" + recordId + ">";
    }

    static String formatId(URI recordId) {
        return "<" + recordId + ">";
    }

    /**
     * The type of record.
     */
    public String type() {
        return headers().sole("WARC-Type").get();
    }

    /**
     * The globally unique identifier for this record.
     */
    public URI id() {
        return parseRecordID(headers().sole("WARC-Record-ID").get());
    }

    /**
     * The instant that data capture for this record began.
     */
    public Instant date() {
        return Instant.parse(headers().sole("WARC-Date").get());
    }

    /**
     * The reason why this record was truncated or {@link WarcTruncationReason#NOT_TRUNCATED}.
     */
    public WarcTruncationReason truncated() {
        return headers().sole("WARC-Truncated")
                .map(value -> WarcTruncationReason.valueOf(value.toUpperCase()))
                .orElse(WarcTruncationReason.NOT_TRUNCATED);
    }

    /**
     * The current record's relative ordering in a sequence of segmented records.
     * <p>
     * Mandatory for all records in the sequence starting from 1.
     */
    public Optional<Long> segmentNumber() {
        return headers().sole("WARC-Segment-Number").map(Long::valueOf);
    }

    /**
     * Digest values that were calculated by applying hash functions to this content body.
     */
    public Optional<WarcDigest> blockDigest() {
        return headers().sole("WARC-Block-Digest").map(WarcDigest::new);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id() + "]";
    }

    @FunctionalInterface
    public interface Constructor<R extends WarcRecord> {
        R construct(MessageVersion version, MessageHeaders headers, MessageBody body);
    }

    @SuppressWarnings("unchecked")
    public abstract static class AbstractBuilder<R extends WarcRecord, B extends AbstractBuilder<R, B>> extends Message.AbstractBuilder<R, B> {
        public AbstractBuilder(String type) {
            super(MessageVersion.WARC_1_0);
            setHeader("WARC-Type", type);
            setHeader("Content-Length", "0");
            date(Instant.now());
            recordId(UUID.randomUUID());
        }

        public B recordId(UUID uuid) {
            return setHeader("WARC-Record-ID", formatId(uuid));
        }

        public B recordId(URI recordId) {
            return setHeader("WARC-Record-ID", formatId(recordId));
        }

        public B date(Instant date) {
            if (version.equals(MessageVersion.WARC_1_0)) {
                date = date.truncatedTo(SECONDS);
            }
            return setHeader("WARC-Date", date.toString());
        }

        public B blockDigest(String algorithm, String value) {
            return blockDigest(new WarcDigest(algorithm, value));
        }

        public B blockDigest(WarcDigest digest) {
            return addHeader("WARC-Block-Digest", digest.prefixedBase32());
        }

        public B truncated(WarcTruncationReason truncationReason) {
            if (truncationReason.equals(WarcTruncationReason.NOT_TRUNCATED)) {
                headerMap.remove("WARC-Truncated");
                return (B) this;
            }
            return addHeader("WARC-Truncated", truncationReason.name().toLowerCase());
        }

        public B segmentNumber(long segmentNumber) {
            return addHeader("WARC-Segment-Number", String.valueOf(segmentNumber));
        }

        protected R build(Constructor<R> constructor) {
            MessageHeaders headers = new MessageHeaders(headerMap);
            return constructor.construct(version, headers, makeBody());
        }
    }
}
