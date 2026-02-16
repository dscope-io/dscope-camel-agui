package io.dscope.camel.agui.samples;

import io.dscope.camel.agui.AgUiComponentApplicationSupport;
import org.apache.camel.builder.RouteBuilder;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        org.apache.camel.main.Main main = createRuntimeMain();
        main.run(args);
    }

    static org.apache.camel.main.Main createRuntimeMain() {
        AgUiComponentApplicationSupport support = new AgUiComponentApplicationSupport();
        return support.createMain("classpath:routes/ag-ui-platform.yaml", main -> {
            if (!Boolean.parseBoolean(System.getProperty("agui.websocket.enabled", "false"))) {
                return;
            }

            String rpcPort = System.getProperty("agui.rpc.port", "8081");
            String wsPath = normalizePath(System.getProperty("agui.websocket.path", "/agui/ws"));
            String wsUri = "undertow:ws://0.0.0.0:" + rpcPort + wsPath + "?sendToAll=false&allowedOrigins=*";

            main.configure().addRoutesBuilder(new RouteBuilder() {
                @Override
                public void configure() {
                    from(wsUri)
                        .routeId("agui-websocket-scaffold")
                        .doTry()
                            .process("agUiJsonRpcEnvelopeProcessor")
                            .process("agUiMethodDispatchProcessor")
                            .to(wsUri)
                        .doCatch(Exception.class)
                            .process("agUiErrorProcessor")
                            .to(wsUri)
                        .end();
                }
            });
        });
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}
