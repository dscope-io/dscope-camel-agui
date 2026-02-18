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

package io.dscope.camel.agui.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiMethodCatalog;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiDiagnosticsProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;
    private final AgUiMethodCatalog methodCatalog;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public AgUiDiagnosticsProcessor(AgUiSessionRegistry sessionRegistry, AgUiMethodCatalog methodCatalog) {
        this.sessionRegistry = sessionRegistry;
        this.methodCatalog = methodCatalog;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getMessage().setHeader("Content-Type", "application/json");
        exchange.getMessage().setBody(
            mapper.writeValueAsString(
                java.util.Map.of(
                    "status", "UP",
                    "activeSessions", sessionRegistry.activeSessionCount(),
                    "supportedMethods", methodCatalog.supportedMethods()
                )
            )
        );
    }
}
