package org.netpreserve.jwarc;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class HttpServer {
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final CertificateAuthority ca;
    private final Handler handler;

    HttpServer(ServerSocket serverSocket, Handler handler) {
        this.serverSocket = serverSocket;
        this.handler = handler;
        try {
            ca = new CertificateAuthority(new X500Principal("Dummy CA"));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Listens and accepts new connections.
     */
    void listen() throws IOException {
        while (!serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            threadPool.execute(() -> interact(socket, ""));
        }
    }

    /**
     * Handles a connection from a client.
     */
    private void interact(Socket socket, String prefix) {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
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
                    handler.handle(socket, prefix + request.target(), request);
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
        send(socket, new HttpResponse.Builder(200, "OK").build());
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

    static void send(Socket socket, HttpResponse response) throws IOException {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.serializeHeader());
            IOUtils.copy(response.body().stream(), outputStream);
        } catch (SSLProtocolException | SocketException e) {
            socket.close(); // client probably closed
        }
    }

    interface Handler {
        void handle(Socket socket, String target, HttpRequest request) throws Exception;
    }
}