package org.netpreserve.jwarc.net;

import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;

import static org.junit.Assert.*;

public class WarcServerTest {
    @Test
    public void test() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // so far just testing that we can instantiate it and load the .js files
            WarcServer server = new WarcServer(serverSocket, Collections.emptyList());
        }
    }
}