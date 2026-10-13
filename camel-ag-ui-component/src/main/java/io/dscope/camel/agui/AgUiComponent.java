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

import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

@Component("agui")
public class AgUiComponent extends DefaultComponent {
  final io.dscope.camel.agui.service.AgUiStateStore standardCheckpoints =
      new io.dscope.camel.agui.service.InMemoryAgUiStateStore();
  final io.dscope.camel.agui.service.AgUiSessionRegistry standardSessions =
      new io.dscope.camel.agui.service.InMemoryAgUiSessionRegistry(
          new io.dscope.camel.agui.service.JacksonAgUiEventCodec());
  final java.util.concurrent.ConcurrentMap<String, java.util.concurrent.locks.ReentrantLock>
      standardThreadLocks = new java.util.concurrent.ConcurrentHashMap<>();

  private java.util.List<java.util.function.UnaryOperator<com.fasterxml.jackson.databind.JsonNode>>
      eventMiddleware = java.util.List.of();

  public java.util.List<java.util.function.UnaryOperator<com.fasterxml.jackson.databind.JsonNode>>
      getEventMiddleware() {
    return eventMiddleware;
  }

  /** Ordered event middleware applied after compatibility translation and before validation. */
  public void setEventMiddleware(
      java.util.List<java.util.function.UnaryOperator<com.fasterxml.jackson.databind.JsonNode>>
          middleware) {
    eventMiddleware = java.util.List.copyOf(middleware);
  }

  @Override
  protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
      throws Exception {
    AgUiConfiguration configuration = new AgUiConfiguration();
    configuration.setAgentId(remaining);
    setProperties(configuration, parameters);
    return new AgUiEndpoint(uri, this, configuration, remaining);
  }
}
