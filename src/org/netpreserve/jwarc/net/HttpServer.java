package org.netpreserve.jwarc.net;

import org.netpreserve.jwarc.HttpRequest;
import org.netpreserve.jwarc.HttpResponse;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;

abstract class HttpServer {
    final ServerSocket serverSocket;
    private final CertificateAuthority ca;
    private final List<Route> routes = new ArrayList<>();

    HttpServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        try {
            ca = new CertificateAuthority(new X500Principal("cn=Dummy CA"));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Listens and accepts new connections.
     */
    public void listen() {
        ExecutorService threadPool = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("HttpServer worker");
            return thread;
        });
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                threadPool.execute(() -> interact(socket, ""));
            }
        } catch (IOException e) {
            // shutdown
        } finally {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(1, SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
    }

    /**
     * Handles a connection from a client.
     */
    private void interact(Socket socket, String prefix) {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.flip();
        try {
            while (!socket.isInputShutdown()) {
                HttpRequest request;
                try {
                    request = HttpRequest.parse(Channels.newChannel(socket.getInputStream()), buffer);
                } catch (SocketException | EOFException e) {
                    return; // client probably closed
                } catch (SSLProtocolException e) {
                    if (e.getCause() instanceof SocketException) {
                        return; // client probably closed
                    }
                    throw e;
                }
                if (request.method().equals("CONNECT")) {
                    upgradeToTls(socket, request.target());
                } else {
                    handle(socket, prefix + request.target(), request);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void upgradeToTls(Socket socket, String target) throws Exception {
        socket.getOutputStream().write(new HttpResponse.Builder(200, "OK").build().serializeHeader());
        String host = target.replaceFirst(":[0-9]+$", "");
        X509Certificate cert = ca.issue(new X500Principal("cn=" + host));
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{new X509KeyManager() {
            public X509Certificate[] getCertificateChain(String alias) {
                return new X509Certificate[]{
                        cert, ca.caCert
                };
            }

            public PrivateKey getPrivateKey(String s) {
                return ca.subKeyPair.getPrivate();
            }

            public String[] getClientAliases(String s, Principal[] principals) {
                throw new IllegalStateException();
            }

            public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                throw new IllegalStateException();
            }

            public String[] getServerAliases(String s, Principal[] principals) {
                return new String[]{host};
            }

            public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
                return host;
            }
        }}, null, null);
        SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, null, true);
        sslSocket.setUseClientMode(false);
        sslSocket.startHandshake();
        interact(sslSocket, "https://" + target.replaceFirst(":443$", ""));
    }

    void handle(Socket socket, String target, HttpRequest request) throws Exception {
        for (Route route : routes) {
            if (route.method != null && !route.method.equalsIgnoreCase(request.method())) continue;
            Matcher matcher = route.pattern.matcher(target);
            if (!matcher.matches()) continue;
            route.handler.handle(new HttpExchange(socket, request, matcher));
        }
    }

    public CertificateAuthority certificateAuthority() {
        return ca;
    }

    private static class Route {
        private final String method;
        Pattern pattern;
        HttpHandler handler;

        Route(String method, String regex, HttpHandler handler) {
            this.method = method;
            pattern = Pattern.compile(regex);
            this.handler = handler;
        }
    }

    protected void on(String method, String regex, HttpHandler handler) {
        routes.add(new Route(method, regex, handler));
    }

}