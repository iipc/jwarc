package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;

import java.io.IOException;
import java.nio.file.Paths;

public class ListTool {
    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            try (WarcReader reader = new WarcReader(Paths.get(arg))) {
                for (WarcRecord record : reader) {
                    String url = "-";
                    if (record instanceof WarcTargetRecord) {
                        url = ((WarcTargetRecord) record).target();
                    }

                    Exception exception = null;
                    String methodOrStatus = "-";
                    if (record.contentType().base().equals(MediaType.HTTP)) {
                        try {
                            if (record instanceof WarcRequest) {
                                methodOrStatus = ((WarcRequest) record).http().method();
                            } else if (record instanceof WarcResponse) {
                                methodOrStatus = String.valueOf(((WarcResponse) record).http().status());
                            }
                        } catch (ParsingException e) {
                            methodOrStatus = "invalid";
                            exception = e;
                        }
                    }

                    System.out.format("%10d %-10s %-4s %s\n", reader.position(), record.type(), methodOrStatus, url);
                    if (exception != null) {
                        System.err.println(exception.getMessage());
                    }
                }
            }
        }
    }
}
