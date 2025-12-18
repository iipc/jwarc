package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.WaczWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WaczTool {
    public static void main(String[] args) throws IOException {
        List<Path> warcFiles = new ArrayList<>();
        Path outputFile = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                    //noinspection JvmTaintAnalysis
                    outputFile = Paths.get(args[++i]);
                    break;
                case "-h":
                case "--help":
                    System.out.println("Usage: jwarc wacz -o output.wacz [warc-files]");
                    System.exit(0);
                    break;
                default:
                    //noinspection JvmTaintAnalysis
                    warcFiles.add(Paths.get(args[i]));
            }
        }
        if (outputFile == null) {
            System.err.println("No output file specified. Try: jwarc wacz --help");
            System.exit(1);
        }
        try (WaczWriter waczWriter = new WaczWriter(Files.newOutputStream(outputFile))) {
            for (Path warcFile : warcFiles) {
                waczWriter.writeResource("archive/" + warcFile.getFileName(), warcFile);
            }
        }
    }
}
