package io.dscope.camel.agui;

import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

@Component("agui")
public class AgUiComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        AgUiConfiguration configuration = new AgUiConfiguration();
        configuration.setAgentId(remaining);
        setProperties(configuration, parameters);
        return new AgUiEndpoint(uri, this, configuration, remaining);
    }
}
