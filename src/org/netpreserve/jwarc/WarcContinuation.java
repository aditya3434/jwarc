/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.net.URI;
import java.util.Optional;

public class WarcContinuation extends WarcTargetRecord {
    WarcContinuation(MessageVersion version, MessageHeaders headers, MessageBody body) {
        super(version, headers, body);
    }

    /**
     * The id of the first record in the series of segments this record is part of.
     */
    public URI segmentOriginId() {
        return headers().sole("WARC-Segment-Origin-ID").map(WarcRecord::parseRecordID).get();
    }

    /**
     * The total length of the content blocks of all segments added together.
     */
    public Optional<Long> segmentTotalLength() {
        return headers().sole("WARC-Segment-Total-Length").map(Long::valueOf);
    }

    public static class Builder extends WarcTargetRecord.Builder<WarcContinuation, Builder> {
        public Builder() {
            super("continuation");
        }

        public Builder segmentOriginId(URI recordId) {
            return setHeader("WARC-Segment-Origin-Id", WarcRecord.formatId(recordId));
        }

        public Builder segmentTotalLength(long segmentTotalLength) {
            return setHeader("WARC-Segment-Total-Length", Long.toString(segmentTotalLength));
        }

        @Override
        public WarcContinuation build() {
            return build(WarcContinuation::new);
        }
    }
}
