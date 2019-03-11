package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.WarcWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class FetchTool {
    public static void main(String[] args) throws IOException, URISyntaxException {
        try (WarcWriter writer = new WarcWriter(System.out)) {
            for (String arg : args) {
                writer.fetch(new URI(arg));
            }
        }
    }
}
