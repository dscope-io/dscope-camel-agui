package io.dscope.camel.agui.processor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiClasspathResourceProcessor implements Processor {

    public static final String DEFAULT_RESOURCE_PATH_HEADER = "AgUiResourcePath";

    private final String resourcePathHeader;
    private final String allowedRootPrefix;
    private final Map<String, String> contentTypes;

    public AgUiClasspathResourceProcessor() {
        this(DEFAULT_RESOURCE_PATH_HEADER, "ui/", Map.of());
    }

    public AgUiClasspathResourceProcessor(String resourcePathHeader, String allowedRootPrefix) {
        this(resourcePathHeader, allowedRootPrefix, Map.of());
    }

    public AgUiClasspathResourceProcessor(String resourcePathHeader, String allowedRootPrefix, Map<String, String> contentTypes) {
        this.resourcePathHeader = blankToDefault(resourcePathHeader, DEFAULT_RESOURCE_PATH_HEADER);
        this.allowedRootPrefix = blankToDefault(allowedRootPrefix, "ui/");
        this.contentTypes = contentTypes == null ? Map.of() : Map.copyOf(contentTypes);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String resourcePath = exchange.getMessage().getHeader(resourcePathHeader, String.class);
        if (resourcePath == null || resourcePath.isBlank() || resourcePath.contains("..") || !resourcePath.startsWith(allowedRootPrefix)) {
            notFound(exchange);
            return;
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
            if (input == null) {
                notFound(exchange);
                return;
            }
            exchange.getMessage().setBody(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, contentType(resourcePath));
        }
    }

    private String contentType(String resourcePath) {
        for (Map.Entry<String, String> entry : contentTypes.entrySet()) {
            if (resourcePath.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (resourcePath.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (resourcePath.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (resourcePath.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (resourcePath.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }
        return "text/plain; charset=UTF-8";
    }

    private void notFound(Exchange exchange) {
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/plain; charset=UTF-8");
        exchange.getMessage().setBody("Not found");
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}