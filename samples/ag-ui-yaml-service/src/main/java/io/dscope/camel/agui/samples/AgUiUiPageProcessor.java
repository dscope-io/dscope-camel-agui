package io.dscope.camel.agui.samples;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

final class AgUiUiPageProcessor implements Processor {

    private static final String RESOURCE_PATH = "static/agui-ui.html";
    private final String html;

    AgUiUiPageProcessor() {
        this.html = loadHtml();
    }

    @Override
    public void process(Exchange exchange) {
        exchange.getMessage().setHeader("Content-Type", "text/html; charset=UTF-8");
        exchange.getMessage().setBody(html);
    }

    private String loadHtml() {
        ClassLoader classLoader = Main.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing UI resource: " + RESOURCE_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load UI resource: " + RESOURCE_PATH, e);
        }
    }
}
