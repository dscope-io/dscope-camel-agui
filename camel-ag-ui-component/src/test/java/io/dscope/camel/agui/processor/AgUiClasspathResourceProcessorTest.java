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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

class AgUiClasspathResourceProcessorTest {

    @Test
    void servesTextResourceWithContentType() throws Exception {
        Exchange exchange = exchangeFor("ui/app.js");

        new AgUiClasspathResourceProcessor().process(exchange);

        assertEquals(200, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("application/javascript; charset=UTF-8", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
        assertEquals("console.log('agui');", exchange.getMessage().getBody(String.class));
    }

    @Test
    void servesBinaryResourceAsBytes() throws Exception {
        Exchange exchange = exchangeFor("ui/font.woff2");

        new AgUiClasspathResourceProcessor().process(exchange);

        assertEquals(200, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("font/woff2", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
        assertArrayEquals("font".getBytes(), exchange.getMessage().getBody(byte[].class));
    }

    @Test
    void rejectsPathOutsideAllowedRoot() throws Exception {
        Exchange exchange = exchangeFor("../application.properties");

        new AgUiClasspathResourceProcessor().process(exchange);

        assertEquals(404, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("Not found", exchange.getMessage().getBody(String.class));
    }

    private Exchange exchangeFor(String resourcePath) {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader(AgUiClasspathResourceProcessor.DEFAULT_RESOURCE_PATH_HEADER, resourcePath);
        return exchange;
    }
}