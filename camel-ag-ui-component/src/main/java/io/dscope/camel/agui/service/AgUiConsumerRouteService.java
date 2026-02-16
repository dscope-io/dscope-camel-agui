package io.dscope.camel.agui.service;

import io.dscope.camel.agui.AgUiEndpoint;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class AgUiConsumerRouteService {

    public void registerRoute(AgUiEndpoint endpoint, Processor processor) throws Exception {
        String uri = endpoint.getConfiguration().getServerUrl() + normalize(endpoint.getConfiguration().getRpcPath());
        String routeId = "agui-consumer-" + Integer.toHexString(uri.hashCode());

        if (endpoint.getCamelContext().getRoute(routeId) != null) {
            return;
        }

        endpoint.getCamelContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(uri)
                    .routeId(routeId)
                    .process(processor);
            }
        });

        if (endpoint.getConfiguration().isWebsocketEnabled()) {
            registerWebSocketRoute(endpoint, processor);
        }
    }

    private void registerWebSocketRoute(AgUiEndpoint endpoint, Processor processor) throws Exception {
        String wsUri = toWebSocketUri(endpoint.getConfiguration().getServerUrl(), endpoint.getConfiguration().getWsPath());
        String routeId = "agui-ws-consumer-" + Integer.toHexString(wsUri.hashCode());
        if (endpoint.getCamelContext().getRoute(routeId) != null) {
            return;
        }

        String sinkUri = appendSendOptions(wsUri, endpoint.getConfiguration().isSendToAll(), endpoint.getConfiguration().getAllowedOrigins());
        endpoint.getCamelContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(wsUri)
                    .routeId(routeId)
                    .process(processor)
                    .to(sinkUri);
            }
        });
    }

    private String normalize(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private String toWebSocketUri(String serverUrl, String wsPath) {
        String normalizedPath = normalize(wsPath);
        String base = serverUrl;
        if (base.startsWith("undertow:http://")) {
            base = "undertow:ws://" + base.substring("undertow:http://".length());
        } else if (base.startsWith("undertow:https://")) {
            base = "undertow:wss://" + base.substring("undertow:https://".length());
        } else if (base.startsWith("http://")) {
            base = "undertow:ws://" + base.substring("http://".length());
        } else if (base.startsWith("https://")) {
            base = "undertow:wss://" + base.substring("https://".length());
        }
        if (!base.startsWith("undertow:ws://") && !base.startsWith("undertow:wss://")) {
            throw new IllegalArgumentException("Unsupported serverUrl for WebSocket conversion: " + serverUrl);
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + normalizedPath;
    }

    private String appendSendOptions(String wsUri, boolean sendToAll, String allowedOrigins) {
        String delimiter = wsUri.contains("?") ? "&" : "?";
        return wsUri + delimiter + "sendToAll=" + sendToAll + "&allowedOrigins=" + allowedOrigins;
    }
}
