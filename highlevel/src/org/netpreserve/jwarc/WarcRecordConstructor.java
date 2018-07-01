/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc;

import java.util.Map;

@FunctionalInterface
public interface WarcRecordConstructor {
    WarcRecord construct(Map<String, String> headers);
}
