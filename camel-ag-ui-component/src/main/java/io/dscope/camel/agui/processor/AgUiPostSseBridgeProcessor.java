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

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import io.dscope.camel.agui.config.AgUiExchangeProperties;

public class AgUiPostSseBridgeProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String runId = resolveRunId(exchange);
        if (runId == null || runId.isBlank()) {
            throw new AgUiJsonRpcValidationException("runId is required for POST+SSE transport");
        }

        exchange.getMessage().setHeader("runId", runId);
        if (exchange.getMessage().getHeader("afterSequence") == null) {
            exchange.getMessage().setHeader("afterSequence", 0L);
        }
        if (exchange.getMessage().getHeader("limit") == null) {
            exchange.getMessage().setHeader("limit", 200);
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveRunId(Exchange exchange) {
        Object paramsObj = exchange.getProperty(AgUiExchangeProperties.PARAMS);
        if (paramsObj instanceof Map<?, ?> params) {
            Object runId = params.get("runId");
            if (runId != null && !runId.toString().isBlank()) {
                return runId.toString();
            }
        }

        Object resultObj = exchange.getProperty(AgUiExchangeProperties.METHOD_RESULT);
        if (resultObj instanceof Map<?, ?> result) {
            Object runId = result.get("runId");
            if (runId != null && !runId.toString().isBlank()) {
                return runId.toString();
            }
        }

        String runId = exchange.getProperty(AgUiExchangeProperties.RUN_ID, String.class);
        if (runId != null && !runId.isBlank()) {
            return runId;
        }
        return null;
    }
}
