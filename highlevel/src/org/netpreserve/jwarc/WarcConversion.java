/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

public class WarcConversion extends WarcTargetRecord {
    WarcConversion(ProtocolVersion version, Headers headers, WarcBody body) {
        super(version, headers, body);
    }

    public abstract static class Builder extends WarcTargetRecord.Builder<WarcConversion, Builder> {
    }
}
