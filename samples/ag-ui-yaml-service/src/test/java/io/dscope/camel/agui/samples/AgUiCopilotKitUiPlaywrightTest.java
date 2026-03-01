/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dscope.camel.agui.samples;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

class AgUiCopilotKitUiPlaywrightTest {

    @Test
    void copilotUiSupportsPostAgentTransport() throws Exception {
        Assumptions.assumeTrue(canLaunchPlaywrightChromium(),
            "Skipping Playwright UI test because Chromium runtime is unavailable");

        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "false");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            String runId = "ui-post-" + UUID.randomUUID();
            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                 BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {

                page.navigate("http://localhost:" + rpcPort + "/agui/ui");
                page.locator("[data-testid='transport-mode']").selectOption("post");
                page.locator("[data-testid='run-id']").fill(runId);
                page.locator("[data-testid='session-id']").fill("session-post");
                page.locator("[data-testid='thread-id']").fill("thread-post");
                page.locator("[data-testid='prompt-input']").fill("weather in berlin");
                page.locator("[data-testid='send-button']").click();

                page.waitForFunction(
                    "() => document.querySelector('[data-testid=\"transport-status\"]').textContent.includes('post-complete')",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(Duration.ofSeconds(20).toMillis()));

                String events = page.locator("[data-testid='event-log']").textContent();
                Assertions.assertNotNull(events);
                Assertions.assertTrue(events.contains("RUN_STARTED"), events);
                Assertions.assertTrue(events.contains("RUN_FINISHED"), events);

                String assistant = page.locator("[data-testid='assistant-log']").textContent();
                Assertions.assertNotNull(assistant);
                Assertions.assertFalse(assistant.isBlank(), assistant);
            }
        } finally {
            runtime.stop();
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
        }
    }

    @Test
    void copilotUiSupportsWebsocketTransport() throws Exception {
        Assumptions.assumeTrue(canLaunchPlaywrightChromium(),
            "Skipping Playwright UI test because Chromium runtime is unavailable");

        int rpcPort = findAvailablePort();
        int healthPort = rpcPort;
        System.setProperty("agui.rpc.port", String.valueOf(rpcPort));
        System.setProperty("agui.health.port", String.valueOf(healthPort));
        System.setProperty("agui.websocket.enabled", "true");
        System.setProperty("agui.websocket.path", "/agui/ws");

        org.apache.camel.main.Main runtime = io.dscope.camel.agui.samples.Main.createRuntimeMain();
        try {
            runtime.start();
            waitForHealth(healthPort);

            String runId = "ui-ws-" + UUID.randomUUID();
            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                 BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {

                page.navigate("http://localhost:" + rpcPort + "/agui/ui");
                page.locator("[data-testid='transport-mode']").selectOption("websocket");
                page.locator("[data-testid='run-id']").fill(runId);
                page.locator("[data-testid='session-id']").fill("session-ws");
                page.locator("[data-testid='thread-id']").fill("thread-ws");
                page.locator("[data-testid='prompt-input']").fill("show me 49ers score");
                page.locator("[data-testid='send-button']").click();

                page.waitForFunction(
                    "() => document.querySelector('[data-testid=\"transport-status\"]').textContent.includes('websocket-complete')",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(Duration.ofSeconds(20).toMillis()));

                String transport = page.locator("[data-testid='transport-log']").textContent();
                Assertions.assertNotNull(transport);
                Assertions.assertTrue(transport.contains("SSE connected"), transport);
                Assertions.assertTrue(transport.contains("Opening WebSocket"), transport);
                Assertions.assertTrue(transport.contains("WS send"), transport);
                Assertions.assertFalse(transport.contains("Transport error"), transport);

                String events = page.locator("[data-testid='event-log']").textContent();
                Assertions.assertNotNull(events);
            }
        } finally {
            runtime.stop();
            System.clearProperty("agui.rpc.port");
            System.clearProperty("agui.health.port");
            System.clearProperty("agui.websocket.enabled");
            System.clearProperty("agui.websocket.path");
        }
    }

    private static boolean canLaunchPlaywrightChromium() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            return browser.isConnected();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void waitForHealth(int port) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:" + port + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
                java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (IOException | InterruptedException ignored) {
            }
            LockSupport.parkNanos(Duration.ofMillis(200).toNanos());
        }
        throw new IllegalStateException("Service did not become healthy on port " + port);
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
