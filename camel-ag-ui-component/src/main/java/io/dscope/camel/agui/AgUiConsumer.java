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

import io.dscope.camel.agui.service.AgUiConsumerRouteService;
import java.util.List;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class AgUiConsumer extends DefaultConsumer {

  private final AgUiEndpoint endpoint;
  private final AgUiConsumerRouteService routeService;
  private List<String> routeIds = List.of();
  private io.dscope.camel.agui.protocol.AgUiStreamingConsumer standardConsumer;

  public AgUiConsumer(AgUiEndpoint endpoint, Processor processor) {
    super(endpoint, processor);
    this.endpoint = endpoint;
    this.routeService = new AgUiConsumerRouteService();
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();
    if ("httpSse".equals(endpoint.getConfiguration().getTransportMode())) {
      String base = endpoint.getConfiguration().getServerUrl();
      if (!base.startsWith("undertow:")) base = "undertow:" + base;
      String path = endpoint.getConfiguration().getAgentPath();
      if (!path.startsWith("/")) path = "/" + path;
      var transport =
          endpoint
              .getCamelContext()
              .getEndpoint(
                  base + path + "?httpMethodRestrict=POST",
                  org.apache.camel.component.undertow.UndertowEndpoint.class);
      var component = (AgUiComponent) endpoint.getComponent();
      var checkpoints =
          endpoint
              .getCamelContext()
              .getRegistry()
              .lookupByNameAndType(
                  "agUiStateStore", io.dscope.camel.agui.service.AgUiStateStore.class);
      var sessions =
          endpoint
              .getCamelContext()
              .getRegistry()
              .lookupByNameAndType(
                  "agUiSessionRegistry", io.dscope.camel.agui.service.AgUiSessionRegistry.class);
      standardConsumer =
          new io.dscope.camel.agui.protocol.AgUiStreamingConsumer(
              transport,
              getProcessor(),
              endpoint,
              checkpoints == null ? component.standardCheckpoints : checkpoints,
              sessions == null ? component.standardSessions : sessions,
              component.standardThreadLocks);
      standardConsumer.start();
      return;
    }
    routeIds = routeService.registerRoute(endpoint, getProcessor());
    for (String routeId : routeIds) {
      endpoint.getCamelContext().getRouteController().startRoute(routeId);
    }
  }

  @Override
  protected void doSuspend() throws Exception {
    if (standardConsumer != null) standardConsumer.suspend();
    super.doSuspend();
  }

  @Override
  protected void doResume() throws Exception {
    if (standardConsumer != null) standardConsumer.resume();
    super.doResume();
  }

  @Override
  protected void doStop() throws Exception {
    if (standardConsumer != null) {
      standardConsumer.stop();
      standardConsumer = null;
    }
    routeService.removeRoutes(endpoint, routeIds);
    routeIds = List.of();
    super.doStop();
  }
}
