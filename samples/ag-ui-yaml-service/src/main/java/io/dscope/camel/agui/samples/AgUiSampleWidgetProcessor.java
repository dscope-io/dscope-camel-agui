package io.dscope.camel.agui.samples;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

final class AgUiSampleWidgetProcessor implements Processor {

    private final List<Processor> delegates = new ArrayList<>();

    AgUiSampleWidgetProcessor() {
        delegates.add(new AgUiWeatherWidgetProcessor());
        delegates.add(new AgUiSportsTickerWidgetProcessor());
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        for (Processor delegate : delegates) {
            delegate.process(exchange);
        }
    }
}