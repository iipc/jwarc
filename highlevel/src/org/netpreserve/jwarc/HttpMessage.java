/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import org.netpreserve.jwarc.parser.ProtocolVersion;

public abstract class HttpMessage extends Message {
    HttpMessage(ProtocolVersion version, Headers headers, Body body) {
        super(version, headers, body);
    }
}
