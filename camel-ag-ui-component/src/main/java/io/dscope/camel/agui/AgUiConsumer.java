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
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class AgUiConsumer extends DefaultConsumer {

    private final AgUiEndpoint endpoint;
    private final AgUiConsumerRouteService routeService;

    public AgUiConsumer(AgUiEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.routeService = new AgUiConsumerRouteService();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        routeService.registerRoute(endpoint, getProcessor());
    }
}
