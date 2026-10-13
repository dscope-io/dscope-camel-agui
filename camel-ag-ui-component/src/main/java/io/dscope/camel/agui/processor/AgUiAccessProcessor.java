package io.dscope.camel.agui.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/** Enforces a bearer token for non-loopback requests to the sample HTTP API. */
public class AgUiAccessProcessor implements Processor {

    private static final String TOKEN_PROPERTY = "agui.access.token";

    @Override
    public void process(Exchange exchange) {
        String configuredToken = System.getProperty(TOKEN_PROPERTY, "").trim();
        String remoteAddress = exchange.getIn().getHeader("CamelHttpRemoteAddress", String.class);
        if (isLoopback(remoteAddress) && configuredToken.isEmpty()) {
            return;
        }

        String authorization = exchange.getIn().getHeader("Authorization", String.class);
        String expected = "Bearer " + configuredToken;
        if (configuredToken.isEmpty() || !expected.equals(authorization)) {
            throw new AgUiAuthorizationException("Access denied");
        }
    }

    private boolean isLoopback(String remoteAddress) {
        return remoteAddress == null || remoteAddress.startsWith("127.0.0.1")
            || remoteAddress.startsWith("[::1]") || remoteAddress.startsWith("0:0:0:0:0:0:0:1");
    }
}
