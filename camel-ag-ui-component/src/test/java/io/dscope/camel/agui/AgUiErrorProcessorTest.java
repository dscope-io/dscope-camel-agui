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
import io.dscope.camel.agui.processor.AgUiErrorProcessor;
import io.dscope.camel.agui.processor.AgUiMethodNotFoundException;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiErrorProcessorTest {

    @Test
    void serializesErrorEnvelope() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty(AgUiExchangeProperties.REQUEST_ID, "x1");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, new AgUiMethodNotFoundException("missing"));

        new AgUiErrorProcessor().process(exchange);

        String body = exchange.getMessage().getBody(String.class);
        Assertions.assertTrue(body.contains("\"code\":-32601"));
        Assertions.assertTrue(body.contains("missing"));
    }
}
