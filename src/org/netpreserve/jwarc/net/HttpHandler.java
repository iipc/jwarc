package org.netpreserve.jwarc.net;

import java.io.IOException;

@FunctionalInterface
public interface HttpHandler {
    void handle(HttpExchange exchange) throws Exception;
}
