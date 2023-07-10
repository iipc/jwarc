/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2023 National Library of Australia and the jwarc contributors
 */
package org.netpreserve.jwarc.cdx;

enum JsonToken {
    FIELD_NAME, START_OBJECT, END_OBJECT, START_ARRAY, END_ARRAY,
    STRING, NUMBER_INT, NUMBER_FLOAT, TRUE, FALSE, NULL
}
