package com.example;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
//import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class HttpsServer {
    public static void main(String[] args) throws Exception {

        System.setProperty("org.eclipse.jetty.server.HttpConfiguration.validateHostHeader", "false");

        // Create a Jetty server instance.
        Server server = new Server();

        // Configure HTTPS settings.
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
        httpConfig.addCustomizer(new SecureRequestCustomizer());
  //      httpConfig.setHttpCompliance(HttpCompliance.UNSAFE);

        // Set up SSL using the keystore.
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("keystore.jks");
        sslContextFactory.setKeyStorePassword("password");
        sslContextFactory.setKeyManagerPassword("password");
	sslContextFactory.setEndpointIdentificationAlgorithm(null);
        sslContextFactory.setSniRequired(false);

        // Set up the HTTP/2 connection factories.
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol("h2");

        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
        HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);

        // Create a connector that listens on all interfaces at port 8443.
        ServerConnector connector = new ServerConnector(server, ssl, alpn, h2, http1);
        connector.setHost("0.0.0.0");
        connector.setPort(8443);
        server.addConnector(connector);

        // Set a handler that delays the response by 200ms.
        server.setHandler(new DelayedHandler());

        // Start the server.
        server.start();
        System.out.println("Server started on https://localhost:8443");
        server.join();
    }
}
