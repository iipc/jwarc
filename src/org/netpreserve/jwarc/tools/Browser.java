package org.netpreserve.jwarc.tools;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Browser {
    static void run(InetSocketAddress proxy, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(System.getenv().getOrDefault("BROWSER", "google-chrome"),
                "--headless", "--disable-gpu", "--disable-breakpad",
                "--ignore-certificate-errors",
                "--proxy-server=" + proxy.getHostString() + ":" + proxy.getPort(),
                "--hide-scrollbars"));
        cmd.addAll(Arrays.asList(args));
        System.err.println(String.join(" ", cmd));
        Process p = new ProcessBuilder(cmd)
                .inheritIO()
                .redirectOutput(new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"))
                .start();
        p.waitFor();
    }
}
