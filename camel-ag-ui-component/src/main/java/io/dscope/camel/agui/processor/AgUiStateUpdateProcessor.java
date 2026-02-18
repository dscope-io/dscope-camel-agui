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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.model.AgUiStateDelta;
import io.dscope.camel.agui.model.AgUiStateSnapshot;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import io.dscope.camel.agui.service.AgUiStateDeltaCalculator;
import io.dscope.camel.agui.service.AgUiStateStore;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiStateUpdateProcessor implements Processor {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AgUiSessionRegistry sessionRegistry;
    private final AgUiStateStore stateStore;
    private final AgUiStateDeltaCalculator deltaCalculator;

    public AgUiStateUpdateProcessor(
        AgUiSessionRegistry sessionRegistry,
        AgUiStateStore stateStore,
        AgUiStateDeltaCalculator deltaCalculator
    ) {
        this.sessionRegistry = sessionRegistry;
        this.stateStore = stateStore;
        this.deltaCalculator = deltaCalculator;
    }

    @Override
    public void process(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = exchange.getProperty(AgUiExchangeProperties.PARAMS, Map.class);
        String runId = params.get("runId").toString();
        Object state = params.getOrDefault("state", Map.of());

        AgUiSession session = requireSession(runId);
        JsonNode previous = stateStore.get(runId);
        JsonNode current = mapper.valueToTree(state);

        stateStore.put(runId, current);
        session.emit(new AgUiStateSnapshot(runId, session.getSessionId(), state));

        var patch = deltaCalculator.toJsonPatch(previous, current);
        if (!patch.isEmpty()) {
            session.emit(new AgUiStateDelta(runId, session.getSessionId(), patch));
        }

        exchange.setProperty(AgUiExchangeProperties.METHOD_RESULT, Map.of("runId", runId, "patchSize", patch.size()));
    }

    private AgUiSession requireSession(String runId) {
        AgUiSession session = sessionRegistry.get(runId);
        if (session == null) {
            throw new AgUiJsonRpcValidationException("Unknown runId: " + runId);
        }
        return session;
    }
}
