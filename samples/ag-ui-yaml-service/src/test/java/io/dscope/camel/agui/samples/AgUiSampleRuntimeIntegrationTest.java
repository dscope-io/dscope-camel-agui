package io.dscope.camel.agui.samples;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiSampleRuntimeIntegrationTest {

    @Test
    void supportsRpcAndSseEndToEnd() throws Exception {
        int healthPort = 8080;
        int rpcPort = 8081;
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            HttpClient http = HttpClient.newHttpClient();
            postJson(http, rpcPort,
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"run.start\",\"params\":{\"runId\":\"it-run\",\"sessionId\":\"it-session\"}}",
                200,
                "\"status\":\"started\"");
            postJson(http, rpcPort,
                "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"method\":\"run.text\",\"params\":{\"runId\":\"it-run\",\"text\":\"hello integration\"}}",
                200,
                "\"textLength\":17");
            postJson(http, rpcPort,
                "{\"jsonrpc\":\"2.0\",\"id\":\"3\",\"method\":\"run.finish\",\"params\":{\"runId\":\"it-run\"}}",
                200,
                "\"status\":\"finished\"");

            HttpRequest sseRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + rpcPort + "/agui/stream/it-run?afterSequence=0&limit=100"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> sseResponse = http.send(sseRequest, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, sseResponse.statusCode());
            Assertions.assertTrue(sseResponse.body().contains("event: run.started"));
            Assertions.assertTrue(sseResponse.body().contains("event: text.message.content"));
            Assertions.assertTrue(sseResponse.body().contains("event: run.finished"));

            HttpRequest badRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + rpcPort + "/agui/rpc"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"jsonrpc\":\"2.0\",\"id\":\"4\",\"method\":\"unknown.method\",\"params\":{}}"))
                .build();
            HttpResponse<String> badResponse = http.send(badRequest, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(400, badResponse.statusCode());
            Assertions.assertTrue(badResponse.body().contains("\"code\":-32601"));
        } finally {
            runtime.stop();
            System.clearProperty("agui.websocket.enabled");
        }
    }

    @Test
    void enablesWebSocketScaffoldingRouteWhenFlagSet() throws Exception {
        int healthPort = 8080;
        int rpcPort = 8081;

        System.setProperty("agui.websocket.enabled", "true");
        System.setProperty("agui.websocket.path", "/agui/ws-test");

        org.apache.camel.main.Main runtime = Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            boolean present = runtime.getCamelContext().getRoutes().stream()
                .anyMatch(route -> "agui-websocket-scaffold".equals(route.getRouteId()));
            Assertions.assertTrue(present);

            HttpClient http = HttpClient.newHttpClient();
            postJson(http, rpcPort,
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"run.start\",\"params\":{\"runId\":\"ws-run\",\"sessionId\":\"ws-session\"}}",
                200,
                "\"status\":\"started\"");
        } finally {
            runtime.stop();
            System.clearProperty("agui.websocket.enabled");
            System.clearProperty("agui.websocket.path");
        }
    }

    private static void postJson(HttpClient http, int rpcPort, String json, int expectedStatus, String expectedBodyToken)
        throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + rpcPort + "/agui/rpc"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(expectedStatus, response.statusCode());
        Assertions.assertTrue(response.body().contains(expectedBodyToken), response.body());
    }

    private static void waitForHealth(int healthPort) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        Exception last = null;
        for (int i = 0; i < 120; i++) {
            try {
                HttpRequest healthRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + healthPort + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
                HttpResponse<String> health = http.send(healthRequest, HttpResponse.BodyHandlers.ofString());
                if (health.statusCode() == 200) {
                    return;
                }
            } catch (Exception e) {
                last = e;
            }
            Thread.sleep(100);
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("Runtime health endpoint did not become ready");
    }

}
