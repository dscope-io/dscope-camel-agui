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

import org.apache.camel.builder.RouteBuilder;

import io.dscope.camel.agui.AgUiComponentApplicationSupport;

public final class Main {

    private static final String AG_UI_AGENT_PRE_RUN_TEXT_PROCESSOR_BEAN = "agUiAgentPreRunTextProcessor";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        org.apache.camel.main.Main main = createRuntimeMain();
        main.run(args);
    }

    static org.apache.camel.main.Main createRuntimeMain() {
        AgUiComponentApplicationSupport support = new AgUiComponentApplicationSupport();
        return support.createMain("classpath:routes/ag-ui-platform.camel.yaml", main -> {
            main.bind("agUiUiPageProcessor", new AgUiUiPageProcessor());
            main.bind("agUiStandardSampleProcessor", new AgUiStandardSampleProcessor());
            main.bind("agUiCapabilitiesProcessor", (org.apache.camel.Processor) exchange -> {
                // Advertise the configured deterministic sample, independently of model discovery.
                var capabilities = io.dscope.camel.agui.protocol.AgUiSchema.MAPPER.readTree("""
                    {"identity":{"name":"Camel AG-UI sample","version":"1.4.0"},
                     "transport":{"streaming":true,"httpBinary":false,"resumable":false},
                     "tools":{"supported":true,"clientProvided":false},
                     "reasoning":{"supported":true,"streaming":true,"encrypted":false},
                     "multiAgent":{"supported":true,"delegation":true,"subagents":[{"name":"documents"},{"name":"summary"}]},
                     "humanInTheLoop":{"supported":true,"approvals":true,"interrupts":true},
                     "output":{"supportedMimeTypes":["text/plain","application/pdf"]}}
                    """);
                io.dscope.camel.agui.protocol.AgUiSchema.validate("AgentCapabilities",capabilities,true);
                exchange.getMessage().setHeader(org.apache.camel.Exchange.CONTENT_TYPE,"application/json");
                exchange.getMessage().setBody(capabilities.toString());
            });
            main.bind(AG_UI_AGENT_PRE_RUN_TEXT_PROCESSOR_BEAN, new AgUiSampleWidgetProcessor());

            if (!Boolean.parseBoolean(System.getProperty("agui.websocket.enabled", "false"))) {
                return;
            }

            String rpcPort = System.getProperty("agui.rpc.port", "8080");
            String wsPath = normalizePath(System.getProperty("agui.websocket.path", "/agui/ws"));
            String wsUri = "undertow:ws://" + System.getProperty("agui.host", "127.0.0.1") + ":" + rpcPort + wsPath + "?sendToAll=false&allowedOrigins=*";

            main.configure().addRoutesBuilder(new RouteBuilder() {
                @Override
                public void configure() {
                    from(wsUri)
                        .routeId("agui-websocket-scaffold")
                        .doTry()
                            .process("agUiAccessProcessor")
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
