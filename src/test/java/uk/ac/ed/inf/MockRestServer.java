package uk.ac.ed.inf;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

/**
 * Minimal mock REST server so you can serve static JSON at given paths for testing.
 */
public class MockRestServer {
    private HttpServer httpServer;

    /**
     * Creates an HttpServer listening on the given port, e.g. 8005.
     */
    public MockRestServer(int port) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    }

    /**
     * Defines a path (e.g. "/orders") which will return the given string as HTTP 200 with text/plain body.
     *
     * @param path the URL path ("/orders", "/actuator/health/livenessState", etc.)
     * @param data the response body you want to return
     * @return the created HttpContext, in case you need to remove it later
     */
    public HttpContext getContextToServeDataOnUrl(String path, String data) {
        return httpServer.createContext(path, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = data.getBytes();
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });
    }

    /**
     * If you want to remove a context mid-test, you can.
     */
    public void removeContext(HttpContext context) {
        httpServer.removeContext(context);
    }

    /**
     * Start the server (non-blocking).
     * Usually called in your @BeforeEach or test setup.
     */
    public void start() {
        httpServer.start();
    }

    /**
     * Stop the server. Usually called in your @AfterEach or test teardown.
     */
    public void stop() {
        httpServer.stop(0);
    }
}
