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