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

package io.dscope.camel.agui;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.processor.AgUiMethodDispatchProcessor;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiMethodDispatchProcessorTest {

    @Test
    void wrapsResultEnvelope() throws Exception {
        Processor p = ex -> ex.setProperty(AgUiExchangeProperties.METHOD_RESULT, Map.of("status", "ok"));
        AgUiMethodDispatchProcessor processor = new AgUiMethodDispatchProcessor(Map.of("health", p));

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty(AgUiExchangeProperties.METHOD, "health");
        exchange.setProperty(AgUiExchangeProperties.ENVELOPE_TYPE, "request");
        exchange.setProperty(AgUiExchangeProperties.REQUEST_ID, "abc");

        processor.process(exchange);

        String body = exchange.getMessage().getBody(String.class);
        Assertions.assertTrue(body.contains("\"jsonrpc\":\"2.0\""));
        Assertions.assertTrue(body.contains("\"id\":\"abc\""));
    }
}
