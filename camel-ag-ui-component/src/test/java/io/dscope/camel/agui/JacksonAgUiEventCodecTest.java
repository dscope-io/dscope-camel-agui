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

import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.agui.service.JacksonAgUiEventCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JacksonAgUiEventCodecTest {

    @Test
    void encodesEventJson() {
        JacksonAgUiEventCodec codec = new JacksonAgUiEventCodec();
        String json = codec.toJson(new AgUiTextMessageContent("run-1", "session-1", "hello"));
        Assertions.assertTrue(json.contains("TEXT_MESSAGE_CONTENT"));
        Assertions.assertTrue(json.contains("hello"));
    }
}
