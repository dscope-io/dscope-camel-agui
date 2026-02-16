package io.dscope.camel.agui;

import org.apache.camel.Category;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

@UriEndpoint(
    firstVersion = "0.1.0",
    scheme = "agui",
    title = "AG-UI",
    syntax = "agui:agentId",
    remote = true,
    category = {Category.AI, Category.RPC}
)
public class AgUiEndpoint extends DefaultEndpoint {

    @UriPath(description = "Target AG-UI agent identifier")
    private String agentId;

    private final AgUiConfiguration configuration;

    @SuppressWarnings("unused")
    private final String remaining;

    public AgUiEndpoint(String endpointUri, Component component, AgUiConfiguration configuration, String remaining) {
        super(endpointUri, component);
        this.configuration = configuration;
        this.agentId = remaining;
        this.configuration.setAgentId(remaining);
        this.remaining = remaining;
    }

    @Override
    public Producer createProducer() {
        return new AgUiProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        return new AgUiConsumer(this, processor);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
        this.configuration.setAgentId(agentId);
    }

    public AgUiConfiguration getConfiguration() {
        return configuration;
    }
}
