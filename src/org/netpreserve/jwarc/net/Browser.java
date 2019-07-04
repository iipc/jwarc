package org.netpreserve.jwarc.net;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;

public class Browser {
    private final String executable;
    private final String userAgent;
    private final InetSocketAddress proxy;
    private final List<String> options = Arrays.asList("--headless", "--disable-gpu", "--ignore-certificate-errors",
            "--hide-scrollbars");
    private final static long DEFAULT_TIMEOUT = 60000;

    public static Browser chrome(String executable, InetSocketAddress proxy) {
        return new Browser(executable, proxy, null);
    }

    Browser(String executable, InetSocketAddress proxy, String userAgent) {
        this.executable = executable;
        this.proxy = proxy;
        this.userAgent = userAgent;
    }

    public void browse(URI uri) throws IOException {
        run(uri.toString());
    }

    public void screenshot(URI uri, Path outfile) throws IOException {
        run("--screenshot=" + outfile, uri.toString());
    }

    public FileChannel screenshot(URI uri) throws IOException {
        Path outfile = Files.createTempFile("jwarc-screenshot", ".png");
        try {
            run("--screenshot=" + outfile, uri.toString());
            return FileChannel.open(outfile, DELETE_ON_CLOSE);
        } catch (Exception e) {
            Files.deleteIfExists(outfile);
            throw e;
        }
    }

    private void run(String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(options);
        if (proxy != null) {
            command.add("--proxy-server=" + proxy.getHostString() + ":" + proxy.getPort());
        }
        if (userAgent != null) {
            command.add("--user-agent=" + userAgent);
        }
        command.addAll(Arrays.asList(args));

        try {
            Process process = new ProcessBuilder(command)
                    .inheritIO()
                    .redirectOutput(devNull())
                    .start();
            if (DEFAULT_TIMEOUT > 0) {
                if (!process.waitFor(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    process.destroy();
                    process.waitFor(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
                    process.destroyForcibly();
                    throw new IOException("timed out after " + DEFAULT_TIMEOUT + "ms");
                }
            } else {
                process.waitFor();
            }
            if (process.exitValue() != 0) {
                throw new IOException("browser returned exit status: " + process.exitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static File devNull() {
        return new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null");
    }
}
