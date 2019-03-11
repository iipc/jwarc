package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.WarcReader;
import org.netpreserve.jwarc.WarcRecord;

import java.io.IOException;
import java.nio.file.Paths;

public class ListTool {
    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            try (WarcReader reader = new WarcReader(Paths.get(arg))) {
                for (WarcRecord record : reader) {
                    System.out.println(record);
                }
            }
        }
    }
}
