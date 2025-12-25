package org.netpreserve.jwarc.tools;

import org.netpreserve.jwarc.*;

import org.netpreserve.jwarc.net.WarcServer;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static java.lang.ProcessBuilder.Redirect.PIPE;

public class ViewTool {
    private static final String GREEN = "\u001B[32m", BLUE = "\u001B[34m", RED = "\u001B[31m", RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String REVERSE = "\u001B[7m", CLEAR = "\u001B[H\u001B[2J";
    private static final String HIDE_CURSOR = "\u001B[?25l", SHOW_CURSOR = "\u001B[?25h";
    private final WarcCaptureReader reader;
    private final Path warcPath;
    private int termRows, termCols;
    private int selectedRow = 0;
    private int scrollOffset = 0;
    private String typeFilter;
    private String statusFilter;
    private String methodFilter;
    private String urlFilter;
    private final List<WarcCapture> rows = new ArrayList<>();
    private boolean splitScreen = false;
    private int selectedRecordIndex = 0;
    private List<WarcCaptureRecord> currentRecords = null;
    private Class<? extends WarcCaptureRecord> preferredRecordClass = null;
    private WarcServer server;
    private int serverPort;

    private ViewTool(WarcReader reader, Path warcPath) {
        this.reader = new WarcCaptureReader(reader);
        this.warcPath = warcPath;
    }

    private void checkTerminalSize() {
        try {
            String[] cmd = new String[]{"stty", "size"};
            Process p = new ProcessBuilder(cmd).inheritIO().redirectOutput(PIPE).start();
            if (p.waitFor() > 0) throw new IOException("Command failed: " + String.join(" ", cmd));
            try (Scanner s = new Scanner(p.getInputStream())) {
                termRows = s.nextInt();
                termCols = s.nextInt();
            }
        } catch (Exception e) {
            termRows = 24;
            termCols = 80;
        }
    }

    private void loadMoreRows() throws IOException {
        while (rows.size() < selectedRow + termRows) {
            WarcCapture capture = reader.next().orElse(null);
            if (capture == null) break;
            if (typeFilter != null && !capture.contentType().map(MediaType::subtype).orElse("").toLowerCase().contains(typeFilter)) {
                continue;
            }
            if (statusFilter != null && !capture.status().map(Objects::toString).orElse("").contains(statusFilter)) {
                continue;
            }
            if (methodFilter != null && !capture.method().orElse("").toLowerCase().contains(methodFilter)) {
                continue;
            }
            if (urlFilter != null && !capture.target().toLowerCase().contains(urlFilter)) {
                continue;
            }

            // preload http response and request headers
            capture.status();
            capture.method();

            rows.add(capture);
        }
    }

    private void reset() throws IOException {
        rows.clear();
        selectedRow = 0;
        scrollOffset = 0;
        reader.position(0);
        loadMoreRows();
    }

    private String readLine(String prompt) throws IOException {
        System.out.print("\r\u001B[K" + prompt);
        System.out.print(SHOW_CURSOR);
        try {
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = System.in.read();
                if (c == '\r' || c == '\n') break;
                if (c == 127 || c == 8) {
                    if (sb.length() > 0) {
                        sb.setLength(sb.length() - 1);
                        System.out.print("\b \b");
                    }
                } else if (c == 27) {
                    return null;
                } else if (c >= 32) {
                    sb.append((char) c);
                    System.out.print((char) c);
                }
                System.out.flush();
            }
            return sb.toString();
        } finally {
            System.out.print(HIDE_CURSOR);
        }
    }

    private void savePayload() throws IOException {
        WarcCapture capture = rows.get(selectedRow);
        WarcCaptureRecord record;
        if (splitScreen && currentRecords != null) {
            record = currentRecords.get(selectedRecordIndex);
        } else {
            record = capture.record();
        }

        Optional<WarcPayload> payload = record.payload();
        if (!payload.isPresent()) {
            System.out.print("\r\u001B[KNo payload for this record. [Press any key]");
            System.out.flush();
            int ignore = System.in.read();
            return;
        }

        String target = capture.target();
        String defaultFilename = "payload.bin";
        try {
            URI uri = URI.create(target);
            String path = uri.getPath();
            if (path != null && !path.isEmpty() && !path.endsWith("/")) {
                defaultFilename = path.substring(path.lastIndexOf('/') + 1);
            }
        } catch (Exception e) {
            // ignore
        }

        String filename = readLine("Save payload to (" + defaultFilename + "): ");
        if (filename == null) return;
        if (filename.isEmpty()) filename = defaultFilename;

        try (MessageBody body = payload.get().body();
             FileChannel out = FileChannel.open(Paths.get(filename), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (body.read(buffer) != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    out.write(buffer);
                }
                buffer.clear();
            }
            System.out.print("\r\u001B[KSaved to " + filename + " [Press any key]");
            System.out.flush();
            int ignore = System.in.read();
        } catch (Exception e) {
            System.out.print("\r\u001B[KError saving payload: " + e.getMessage() + " [Press any key]");
            System.out.flush();
            int ignore = System.in.read();
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + "B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        char unit = "KMGT".charAt(exp - 1);
        double v = size / Math.pow(1024, exp);
        return String.format(v < 10 ? "%.1f%s" : "%.0f%s", v, unit);
    }

    private void renderRow(WarcCapture capture, boolean selected) throws IOException {
        int typeLen = 7;
        int status = capture.status().orElse(0);
        String statusColor = status >= 400 ? RED : status >= 300 ? BLUE : GREEN;

        int urlWidth = termCols - 24 - 3 - 4 - 7 - typeLen - 4 - 2; // 4 spaces for separators
        if (urlWidth < 0) urlWidth = 0;
        String type = capture.contentType().map(MediaType::subtype).orElse("");
        long size = capture.record().body().size();

        if ("javascript".equals(type)) type = "js";
        if (selected) System.out.print(REVERSE);
        System.out.printf("%-24s %s%-3s" + RESET + (selected ? REVERSE : "") + " %4s %-" + typeLen + "s %4s %-" + urlWidth + "s" + RESET + "\r%n",
                capture.date(),
                statusColor,
                capture.status().map(Objects::toString).orElse(""),
                formatSize(size),
                trunc(type, typeLen),
                capture.method().orElse(""),
                trunc(capture.target(), urlWidth));
    }

    private String trunc(String s, int len) {
        if (s == null) return "";
        return s.substring(0, Math.min(s.length(), len));
    }

    private String colorizeHeader(String line) {
        int space = line.indexOf(' ');
        int colon = line.indexOf(':');
        if (colon != -1 && (space == -1 || colon < space)) {
            return CYAN + line.substring(0, colon) + RESET + line.substring(colon);
        }
        return line;
    }

    private void openBrowser() throws IOException {
        if (server == null) {
            ServerSocket serverSocket = new ServerSocket(0);
            this.serverPort = serverSocket.getLocalPort();
            server = new WarcServer(serverSocket, Collections.singletonList(warcPath));
            new Thread(() -> server.listen()).start();
        }
        WarcCapture capture = rows.get(selectedRow);
        DateTimeFormatter ARC_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
        String url = "http://localhost:" + serverPort + "/replay/" + ARC_DATE.format(capture.date()) + "/" + capture.target();
        boolean opened = false;
        try {
            if (System.getProperty("os.name").startsWith("Mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url}).waitFor();
                opened = true;
            } else if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported() &&
                       Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                opened = true;
            }
        } catch (Exception e) {
            // ignore and fallback to printing URL
        }

        if (!opened) {
            System.out.print("\r\u001B[KOpen URL: " + url + " [Press any key]");
            System.out.flush();
            System.in.read();
        }
    }

    private void render() throws IOException, InterruptedException {
        checkTerminalSize();
        loadMoreRows();
        if (rows.isEmpty()) {
            System.out.println("No captures found.");
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.print(SHOW_CURSOR);
                new ProcessBuilder("stty", "-raw", "echo").inheritIO().start().waitFor();
                if (server != null) server.close();
            } catch (Exception e) {
                // ignore
            }
        }));
        new ProcessBuilder("stty", "raw", "-echo").inheritIO().start().waitFor();
        System.out.print(HIDE_CURSOR);
        while (true) {
            int height = termRows - 2; // -1 for toolbar, -1 for padding/status
            if (splitScreen) height /= 4;
            boolean anyFilter = typeFilter != null || statusFilter != null || methodFilter != null || urlFilter != null;
            if (anyFilter) height--;
            System.out.print(CLEAR);
            for (int i = scrollOffset; i < Math.min(rows.size(), scrollOffset + height); i++) {
                renderRow(rows.get(i), i == selectedRow);
            }
            if (anyFilter) {
                StringBuilder sb = new StringBuilder("\rFilter:");
                if (typeFilter != null) sb.append(" type:").append(typeFilter);
                if (statusFilter != null) sb.append(" status:").append(statusFilter);
                if (methodFilter != null) sb.append(" method:").append(methodFilter);
                if (urlFilter != null) sb.append(" url:").append(urlFilter);
                System.out.print(sb + "\r\n");
            }

            if (splitScreen) {
                if (currentRecords == null) {
                    currentRecords = rows.get(selectedRow).records();
                    selectedRecordIndex = 0;
                    if (preferredRecordClass != null) {
                        for (int i = 0; i < currentRecords.size(); i++) {
                            if (preferredRecordClass.isInstance(currentRecords.get(i))) {
                                selectedRecordIndex = i;
                                break;
                            }
                        }
                    }
                }
                System.out.print("\r\n");
                for (int i = 0; i < currentRecords.size(); i++) {
                    WarcCaptureRecord record = currentRecords.get(i);
                    String label = record.getClass().getSimpleName().replace("Warc", "");
                    if (i == selectedRecordIndex) {
                        System.out.print(REVERSE + " " + label + " " + RESET + " ");
                    } else {
                        System.out.print(" " + label + "  ");
                    }
                }
                System.out.print("\r\n\r\n");
                WarcCaptureRecord selectedRecord = currentRecords.get(selectedRecordIndex);
                StringBuilder detailBuilder = new StringBuilder();
                detailBuilder.append(new String(selectedRecord.serializeHeader()));
                try {
                    if (selectedRecord instanceof WarcResponse) {
                        detailBuilder.append("\r\n");
                        detailBuilder.append(new String(((WarcResponse) selectedRecord).http().serializeHeader()));
                    } else if (selectedRecord instanceof WarcRequest) {
                        detailBuilder.append("\r\n");
                        detailBuilder.append(new String(((WarcRequest) selectedRecord).http().serializeHeader()));
                    } else if (selectedRecord instanceof WarcRevisit) {
                        detailBuilder.append("\r\n");
                        detailBuilder.append(new String(((WarcRevisit) selectedRecord).http().serializeHeader()));
                    }
                } catch (IOException e) {
                    // ignore
                }
                String[] lines = detailBuilder.toString().split("\r?\n");
                int remainingHeight = termRows - height - (anyFilter ? 1 : 0) - 5; // 5 for toolbar, tabs, and spacing
                for (int i = 0; i < Math.min(lines.length, remainingHeight); i++) {
                    System.out.print(trunc(colorizeHeader(lines[i]), termCols) + "\r\n");
                }
                for (int i = lines.length; i < remainingHeight; i++) {
                    System.out.print("\r\n");
                }
            }

            System.out.print("\u001B[7m q:quit f:filter s:save b:browser ↑↓:scroll PgUp/PgDn <ret>:details \u001B[0m");
            int c = System.in.read();
            if (c == 'q' || c == 3) break;
            if (c == 'b') {
                openBrowser();
                continue;
            }
            if (c == 's') {
                savePayload();
                continue;
            }
            if (c == '\r' || c == '\n') {
                splitScreen = !splitScreen;
                currentRecords = null;
                if (splitScreen && preferredRecordClass == null) {
                    // force loading records to set initial preferred class if not set
                    try {
                        List<WarcCaptureRecord> records = rows.get(selectedRow).records();
                        if (!records.isEmpty()) {
                            preferredRecordClass = records.get(0).getClass();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
                continue;
            }
            if (c == '\t') {
                if (splitScreen && currentRecords != null) {
                    selectedRecordIndex = (selectedRecordIndex + 1) % currentRecords.size();
                    preferredRecordClass = currentRecords.get(selectedRecordIndex).getClass();
                }
                continue;
            }
            if (c == 'f') {
                System.out.print("\r\u001B[KFilter (t:type s:status m:method u:url c:clear): ");
                System.out.flush();
                int c2 = System.in.read();
                if (c2 == 't') {
                    String filter = readLine("type: ");
                    if (filter != null) {
                        typeFilter = filter.isEmpty() ? null : filter.toLowerCase();
                        reset();
                    }
                } else if (c2 == 's') {
                    String filter = readLine("status: ");
                    if (filter != null) {
                        statusFilter = filter.isEmpty() ? null : filter;
                        reset();
                    }
                } else if (c2 == 'm') {
                    String filter = readLine("method: ");
                    if (filter != null) {
                        methodFilter = filter.isEmpty() ? null : filter.toLowerCase();
                        reset();
                    }
                } else if (c2 == 'u') {
                    String filter = readLine("url: ");
                    if (filter != null) {
                        urlFilter = filter.isEmpty() ? null : filter.toLowerCase();
                        reset();
                    }
                } else if (c2 == 'c') {
                    typeFilter = null;
                    statusFilter = null;
                    methodFilter = null;
                    urlFilter = null;
                    reset();
                }
                continue;
            }
            if (c == '\u001B') {
                int c2 = System.in.read();
                if (c2 == '[') {
                    int c3 = System.in.read();
                    if (c3 == 'A') { // Up
                        selectedRow = Math.max(0, selectedRow - 1);
                        currentRecords = null;
                    } else if (c3 == 'B') { // Down
                        selectedRow = Math.min(rows.size(), selectedRow + 1);
                        loadMoreRows();
                        if (selectedRow >= rows.size()) selectedRow = rows.size() - 1;
                        currentRecords = null;
                    } else if (c3 == '5') { // Page Up
                        if (System.in.read() == '~') {
                            selectedRow = Math.max(0, selectedRow - height);
                            currentRecords = null;
                        }
                    } else if (c3 == '6') { // Page Down
                        if (System.in.read() == '~') {
                            selectedRow = selectedRow + height;
                            loadMoreRows();
                            if (selectedRow >= rows.size()) selectedRow = rows.size() - 1;
                            currentRecords = null;
                        }
                    }
                }
            }
            if (selectedRow < scrollOffset) {
                scrollOffset = selectedRow;
            } else if (selectedRow >= scrollOffset + height) {
                scrollOffset = selectedRow - height + 1;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //noinspection JvmTaintAnalysis
        Path path = Paths.get(args[0]);
        try (WarcReader reader = new WarcReader(path)) {
            new ViewTool(reader, path).render();
        }
    }
}
