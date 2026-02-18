package io.dscope.camel.agui.samples;

import java.io.IOException;
import java.net.URI;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiSampleRuntimeIntegrationTest {

    @Test
    void supportsRpcAndSseEndToEnd() throws Exception {
        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
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
            Assertions.assertTrue(sseResponse.body().contains("event: RUN_STARTED"));
            Assertions.assertTrue(sseResponse.body().contains("event: TEXT_MESSAGE_CONTENT"));
            Assertions.assertTrue(sseResponse.body().contains("event: RUN_FINISHED"));

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
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
        }
    }

    @Test
    void enablesWebSocketScaffoldingRouteWhenFlagSet() throws Exception {
        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;

        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "true");
        System.setProperty("agui.websocket.path", "/agui/ws-test");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
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
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
            System.clearProperty("agui.websocket.path");
        }
    }

    @Test
    void supportsSingleEndpointPostSseTransport() throws Exception {
        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest postSseRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + rpcPort + "/agui/agent"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"jsonrpc\":\"2.0\",\"id\":\"5\",\"method\":\"run.start\",\"params\":{\"runId\":\"post-sse-run\",\"sessionId\":\"post-sse-session\"}}"))
                .build();
            HttpResponse<String> postSseResponse = http.send(postSseRequest, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, postSseResponse.statusCode());
            String contentType = postSseResponse.headers().firstValue("Content-Type").orElse("");
            Assertions.assertTrue(contentType.contains("text/event-stream"), contentType);
            Assertions.assertTrue(postSseResponse.body().contains("event: RUN_STARTED"));
            Assertions.assertTrue(postSseResponse.body().contains("event: STEP_STARTED"));
        } finally {
            runtime.stop();
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
        }
    }

    @Test
    void supportsSingleEndpointPostSseWithMethodlessAgUiPayload() throws Exception {
        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest postSseRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + rpcPort + "/agui/agent"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"runId\":\"methodless-run\",\"sessionId\":\"methodless-session\",\"messages\":[{\"role\":\"user\",\"content\":\"hello dojo\"}]}"))
                .build();
            HttpResponse<String> postSseResponse = http.send(postSseRequest, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, postSseResponse.statusCode());
            String contentType = postSseResponse.headers().firstValue("Content-Type").orElse("");
            Assertions.assertTrue(contentType.contains("text/event-stream"), contentType);
            Assertions.assertTrue(postSseResponse.body().contains("event: RUN_STARTED"));
            Assertions.assertTrue(postSseResponse.body().contains("event: STEP_STARTED"));
            Assertions.assertTrue(postSseResponse.body().contains("event: TEXT_MESSAGE_START"));
            Assertions.assertTrue(postSseResponse.body().contains("event: TEXT_MESSAGE_CONTENT"));
            Assertions.assertTrue(postSseResponse.body().contains("hello dojo"));
            Assertions.assertTrue(postSseResponse.body().contains("\"threadId\":"));
            Assertions.assertTrue(postSseResponse.body().contains("event: TEXT_MESSAGE_END"));
            Assertions.assertTrue(postSseResponse.body().contains("event: RUN_FINISHED"));
        } finally {
            runtime.stop();
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
        }
    }

    @Test
    void emitsWeatherToolLifecycleForMethodlessAgentRequest() throws Exception {
        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest postSseRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + rpcPort + "/agui/agent"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"threadId\":\"weather-thread\",\"messages\":[{\"role\":\"user\",\"content\":\"what is the weather in Berlin?\"}]}"))
                .build();
            HttpResponse<String> postSseResponse = http.send(postSseRequest, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, postSseResponse.statusCode());
            String contentType = postSseResponse.headers().firstValue("Content-Type").orElse("");
            Assertions.assertTrue(contentType.contains("text/event-stream"), contentType);

            String body = postSseResponse.body();
            Assertions.assertTrue(body.contains("event: TOOL_CALL_START"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_ARGS"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_RESULT"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_END"));
            Assertions.assertTrue(
                body.contains("\"toolCallName\":\"get_weather\"")
                    || body.contains("\"toolName\":\"get_weather\""),
                body);
            Assertions.assertTrue(body.contains("\"toolCallId\":"));
            Assertions.assertTrue(body.contains("\"delta\":"));
            Assertions.assertTrue(body.contains("\"content\":"));
            Assertions.assertTrue(body.contains("Weather in Berlin: 18C and Cloudy."));
            Assertions.assertTrue(body.contains("event: TEXT_MESSAGE_CONTENT"));
            Assertions.assertTrue(body.contains("event: RUN_FINISHED"));
        } finally {
            runtime.stop();
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
        }
    }

    @Test
    void emitsSportsTickerForMethodlessAgentRequest() throws Exception {
        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest postSseRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + rpcPort + "/agui/agent"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"threadId\":\"sports-thread\",\"messages\":[{\"role\":\"user\",\"content\":\"show score for San Francisco 49ers vs Dallas Cowboys\"}]}"))
                .build();
            HttpResponse<String> postSseResponse = http.send(postSseRequest, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, postSseResponse.statusCode());
            String contentType = postSseResponse.headers().firstValue("Content-Type").orElse("");
            Assertions.assertTrue(contentType.contains("text/event-stream"), contentType);

            String body = postSseResponse.body();
            Assertions.assertTrue(body.contains("event: TOOL_CALL_START"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_ARGS"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_RESULT"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_END"));
            Assertions.assertTrue(
                body.contains("\"toolCallName\":\"get_score\"")
                    || body.contains("\"toolName\":\"get_score\""),
                body);
            Assertions.assertTrue(body.contains("\\\"widgetType\\\":\\\"score_card\\\""));
            Assertions.assertTrue(body.contains("\\\"league\\\":\\\"NFL\\\""));
            Assertions.assertTrue(body.contains("\\\"homeTeam\\\":\\\"San Francisco 49ers\\\""));
            Assertions.assertTrue(body.contains("\\\"awayTeam\\\":\\\"Dallas Cowboys\\\""));
            Assertions.assertTrue(body.contains("\\\"homeScore\\\":40"));
            Assertions.assertTrue(body.contains("\\\"awayScore\\\":3"));
            Assertions.assertTrue(body.contains("\\\"status\\\":\\\"Final\\\""));
            Assertions.assertTrue(body.contains("San Francisco 49ers are winning 40-3 against the Dallas Cowboys."));
            Assertions.assertTrue(body.contains("event: TEXT_MESSAGE_CONTENT"));
            Assertions.assertTrue(body.contains("event: RUN_FINISHED"));
        } finally {
            runtime.stop();
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
        }
    }

    @Test
    void supportsBackendToolRenderingAliasEndpoint() throws Exception {
        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest postSseRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + rpcPort + "/agui/backend_tool_rendering"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"threadId\":\"btr-thread\",\"messages\":[{\"role\":\"user\",\"content\":\"what is the weather in Berlin?\"}]}"))
                .build();
            HttpResponse<String> postSseResponse = http.send(postSseRequest, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, postSseResponse.statusCode());
            String contentType = postSseResponse.headers().firstValue("Content-Type").orElse("");
            Assertions.assertTrue(contentType.contains("text/event-stream"), contentType);

            String body = postSseResponse.body();
            Assertions.assertTrue(body.contains("event: RUN_STARTED"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_START"));
            Assertions.assertTrue(body.contains("event: TOOL_CALL_RESULT"));
            Assertions.assertTrue(
                body.contains("\"toolCallName\":\"get_weather\"")
                    || body.contains("\"toolName\":\"get_weather\""),
                body);
            Assertions.assertTrue(body.contains("Weather in Berlin: 18C and Cloudy."));
            Assertions.assertTrue(body.contains("event: RUN_FINISHED"));
        } finally {
            runtime.stop();
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
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
