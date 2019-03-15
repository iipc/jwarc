package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;
import org.netpreserve.jwarc.net.CaptureIndex;
import org.netpreserve.jwarc.net.WarcRenderer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScreenshotTool {
    public static void main(String[] args) throws Exception {
        List<Path> warcs = Stream.of(args).map(Paths::get).collect(Collectors.toList());
        try (WarcWriter warcWriter = new WarcWriter(System.out);
             WarcRenderer renderer = new WarcRenderer(new CaptureIndex(warcs))) {
            for (String arg : args) {
                try (WarcReader reader = new WarcReader(Paths.get(arg))) {
                    for (WarcRecord record : reader) {
                        if (!isNormalPage(record)) continue;
                        WarcCaptureRecord capture = (WarcCaptureRecord) record;
                        renderer.screenshot(capture.targetURI(), capture.date(), warcWriter);
                    }
                }
            }
        }
    }

    private static boolean isNormalPage(WarcRecord record) throws IOException {
        if (!(record instanceof WarcResponse) && !(record instanceof WarcResource)) {
            return false;
        }
        WarcCaptureRecord capture = (WarcCaptureRecord) record;
        String scheme = capture.targetURI().getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return false;
        }
        try {
            if (!(capture.payload().isPresent() && capture.payload().get().type().base().equals(MediaType.HTML))) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return !(capture instanceof WarcResponse) || ((WarcResponse) capture).http().status() == 200;
    }
}
