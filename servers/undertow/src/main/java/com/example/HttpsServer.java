package com.example;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpsServer {
    // A scheduler to delay the response
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws Exception {
        // Load keystore and initialize SSL context
        String keyStorePath = "keystore.jks";
        char[] password = "password".toCharArray();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream keyStoreStream = new FileInputStream(keyStorePath)) {
            keyStore.load(keyStoreStream, password);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        // Create an HTTP handler that waits 200ms before sending the response.
        HttpHandler handler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) {
                // Dispatch to a worker thread to avoid blocking the IO thread.
                exchange.dispatch();

                // Schedule the response to be sent after 200 milliseconds.
                scheduler.schedule(() -> {
                    exchange.getResponseSender().send("hello undertow!");
                }, 50, TimeUnit.MILLISECONDS);
            }
        };

        // Build and start the Undertow server with HTTPS and HTTP/2 enabled.
        Undertow server = Undertow.builder()
                .addHttpsListener(8443, "0.0.0.0", sslContext)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .setHandler(handler)
                .build();

        server.start();
        System.out.println("Server started on https://localhost:8443");
    }
}
