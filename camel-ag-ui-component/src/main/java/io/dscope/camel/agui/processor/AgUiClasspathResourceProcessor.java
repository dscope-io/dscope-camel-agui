package io.dscope.camel.agui.processor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiClasspathResourceProcessor implements Processor {

    public static final String DEFAULT_RESOURCE_PATH_HEADER = "AgUiResourcePath";

    private static final Map<String, String> DEFAULT_CONTENT_TYPES = Map.ofEntries(
        Map.entry(".html", "text/html; charset=UTF-8"),
        Map.entry(".htm", "text/html; charset=UTF-8"),
        Map.entry(".css", "text/css; charset=UTF-8"),
        Map.entry(".js", "application/javascript; charset=UTF-8"),
        Map.entry(".mjs", "application/javascript; charset=UTF-8"),
        Map.entry(".json", "application/json; charset=UTF-8"),
        Map.entry(".map", "application/json; charset=UTF-8"),
        Map.entry(".webmanifest", "application/manifest+json; charset=UTF-8"),
        Map.entry(".txt", "text/plain; charset=UTF-8"),
        Map.entry(".md", "text/markdown; charset=UTF-8"),
        Map.entry(".csv", "text/csv; charset=UTF-8"),
        Map.entry(".xml", "application/xml; charset=UTF-8"),
        Map.entry(".svg", "image/svg+xml; charset=UTF-8"),
        Map.entry(".png", "image/png"),
        Map.entry(".jpg", "image/jpeg"),
        Map.entry(".jpeg", "image/jpeg"),
        Map.entry(".gif", "image/gif"),
        Map.entry(".webp", "image/webp"),
        Map.entry(".avif", "image/avif"),
        Map.entry(".ico", "image/x-icon"),
        Map.entry(".wasm", "application/wasm"),
        Map.entry(".woff", "font/woff"),
        Map.entry(".woff2", "font/woff2"),
        Map.entry(".ttf", "font/ttf"),
        Map.entry(".otf", "font/otf"),
        Map.entry(".eot", "application/vnd.ms-fontobject"),
        Map.entry(".pdf", "application/pdf")
    );

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
            byte[] bytes = input.readAllBytes();
            String contentType = contentType(resourcePath);
            exchange.getMessage().setBody(isTextContent(contentType) ? new String(bytes, StandardCharsets.UTF_8) : bytes);
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, contentType);
        }
    }

    private String contentType(String resourcePath) {
        String normalized = resourcePath.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : contentTypes.entrySet()) {
            if (normalized.endsWith(entry.getKey().toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        for (Map.Entry<String, String> entry : DEFAULT_CONTENT_TYPES.entrySet()) {
            if (normalized.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "text/plain; charset=UTF-8";
    }

    private boolean isTextContent(String contentType) {
        return contentType.startsWith("text/")
            || contentType.contains("charset=")
            || contentType.startsWith("application/json")
            || contentType.startsWith("application/javascript")
            || contentType.startsWith("application/xml");
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