/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.testsuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface TestReader {

    default TestRecord read(String resource) throws IOException {
        return read(TestReader.class.getResourceAsStream(resource));
    }

    TestRecord read(InputStream stream) throws IOException;
}
