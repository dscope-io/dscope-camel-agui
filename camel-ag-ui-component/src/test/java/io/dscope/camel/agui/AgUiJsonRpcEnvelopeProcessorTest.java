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
import io.dscope.camel.agui.processor.AgUiJsonRpcEnvelopeProcessor;
import io.dscope.camel.agui.processor.AgUiJsonRpcValidationException;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiJsonRpcEnvelopeProcessorTest {

    @Test
    void parsesValidEnvelope() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(Map.of(
            "jsonrpc", "2.0",
            "id", "1",
            "method", "run.start",
            "params", Map.of("runId", "r1")
        ));

        new AgUiJsonRpcEnvelopeProcessor().process(exchange);

        Assertions.assertEquals("run.start", exchange.getProperty(AgUiExchangeProperties.METHOD));
        Assertions.assertEquals("request", exchange.getProperty(AgUiExchangeProperties.ENVELOPE_TYPE));
    }

    @Test
    void rejectsInvalidMethod() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(Map.of(
            "jsonrpc", "2.0",
            "id", "1",
            "method", "unknown.method",
            "params", Map.of()
        ));

        Assertions.assertThrows(RuntimeException.class, () -> new AgUiJsonRpcEnvelopeProcessor().process(exchange));
    }

    @Test
    void rejectsInvalidVersion() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(Map.of(
            "jsonrpc", "1.0",
            "id", "1",
            "method", "run.start",
            "params", Map.of()
        ));

        Assertions.assertThrows(AgUiJsonRpcValidationException.class, () -> new AgUiJsonRpcEnvelopeProcessor().process(exchange));
    }
}
