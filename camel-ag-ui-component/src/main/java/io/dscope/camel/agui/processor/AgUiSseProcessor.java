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

import io.dscope.camel.agui.service.AgUiSessionEventRecord;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiSseProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;

    public AgUiSseProcessor(AgUiSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void process(Exchange exchange) {
        String runId = exchange.getIn().getHeader("runId", String.class);
        if (runId == null || runId.isBlank()) {
            runId = exchange.getIn().getHeader("CamelHttpPath", String.class);
            if (runId != null && runId.contains("/")) {
                runId = runId.substring(runId.lastIndexOf('/') + 1);
            }
        }

        Long afterSequence = exchange.getIn().getHeader("afterSequence", Long.class);
        Integer limit = exchange.getIn().getHeader("limit", Integer.class);

        long from = afterSequence == null ? 0L : afterSequence;
        int max = limit == null ? 200 : limit;

        List<AgUiSessionEventRecord> events = sessionRegistry.eventsSince(runId, from, max);
        StringBuilder body = new StringBuilder();
        for (AgUiSessionEventRecord event : events) {
            body.append("id: ").append(event.sequence()).append('\n');
            body.append("event: ").append(event.eventType()).append('\n');
            body.append("data: ").append(event.json()).append("\n\n");
        }

        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "text/event-stream");
        exchange.getMessage().setHeader("Cache-Control", "no-cache");
        exchange.getMessage().setBody(body.toString());
    }
}
