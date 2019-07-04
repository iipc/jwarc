package org.netpreserve.jwarc.net;

import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.HttpResponse;
import org.netpreserve.jwarc.IOUtils;
import org.netpreserve.jwarc.MediaType;

import javax.net.ssl.SSLProtocolException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Matcher;

import static java.nio.charset.StandardCharsets.UTF_8;

class HttpExchange {
    private static final MediaType HTML_UTF8 = MediaType.parse("text/html;charset=utf-8");

    private final Socket socket;
    private final HttpRequest request;
    private final Matcher matcher;

    HttpExchange(Socket socket, HttpRequest request, Matcher matcher) {
        this.socket = socket;
        this.request = request;
        this.matcher = matcher;
    }

    public String param(int i) {
        return matcher.group(i);
    }

    public HttpRequest request() {
        return request;
    }

    public void redirect(String location) throws IOException {
        send(new HttpResponse.Builder(307, "Redirect")
                .addHeader("Content-Length", "0")
                .addHeader("Location", location)
                .build());
    }

    public void send(int status, String html) throws IOException {
        send(status, HTML_UTF8, html);
    }

    public void send(int status, MediaType type, String body) throws IOException {
        send(new HttpResponse.Builder(status, " ").body(type, body.getBytes(UTF_8)).build());
    }

    public void send(HttpResponse response) throws IOException {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.serializeHeader());
            IOUtils.copy(response.body().stream(), outputStream);
        } catch (SSLProtocolException | SocketException e) {
            socket.close(); // client probably closed
        }
    }
}
