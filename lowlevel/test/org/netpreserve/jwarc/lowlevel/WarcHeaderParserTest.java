/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2018 National Library of Australia and the jwarc contributors
 */

package org.netpreserve.jwarc.lowlevel;

import org.junit.Test;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WarcHeaderParserTest {

    @Test
    public void test() {
        WarcHeaderParser parser = new WarcHeaderParser(handler);
        parser.update(ByteBuffer.wrap(StandardExamples.resource.getBytes(UTF_8)));
    }

}