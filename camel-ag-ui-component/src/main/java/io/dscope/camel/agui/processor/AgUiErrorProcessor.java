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
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.config.AgUiProtocolDefaults;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiErrorProcessor implements Processor {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        if (throwable == null) {
            throwable = exchange.getException();
        }

        int code = throwable instanceof AgUiAuthorizationException ? -32001
            : throwable instanceof AgUiMethodNotFoundException ? -32601
            : throwable instanceof AgUiJsonRpcValidationException ? -32602 : -32603;
        String message = code == -32001 ? "Access denied" : code == -32601 ? "Method not found"
            : code == -32602 ? "Invalid request parameters" : "Internal server error";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", AgUiProtocolDefaults.JSONRPC_VERSION);
        payload.put("id", exchange.getProperty(AgUiExchangeProperties.REQUEST_ID));
        payload.put("error", Map.of("code", code, "message", message));

        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, code == -32001 ? 403 : code == -32603 ? 500 : 400);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getMessage().setBody(mapper.writeValueAsString(payload));
        exchange.setException(null);
    }
}
