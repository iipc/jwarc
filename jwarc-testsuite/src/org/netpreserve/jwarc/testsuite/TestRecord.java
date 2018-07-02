/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.testsuite;

import java.util.Map;

public interface TestRecord {
    String getHeader(String name);
    Map<String,String> getHeaders();
}
