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
