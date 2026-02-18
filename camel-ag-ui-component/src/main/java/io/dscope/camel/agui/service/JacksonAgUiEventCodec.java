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

package io.dscope.camel.agui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiEvent;

public class JacksonAgUiEventCodec implements AgUiEventCodec {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public String toJson(AgUiEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode AG-UI event", e);
        }
    }
}
