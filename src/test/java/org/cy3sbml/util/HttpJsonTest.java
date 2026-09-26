package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpJsonTest {
    private HttpServer server;
    private HttpJson httpJson;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/malformed", exchange -> {
            byte[] body = "{not json".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/error", exchange -> {
            byte[] body = new byte[0];
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        httpJson = new HttpJson(java.net.http.HttpClient.newHttpClient(), new ObjectMapper());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
    }

    @Test
    void returnsEmptyOnMalformedJson() {
        assertTrue(httpJson.get(uri("/malformed")).isEmpty());
    }

    @Test
    void returnsEmptyOnHttpError() {
        assertTrue(httpJson.get(uri("/error")).isEmpty());
    }
}
