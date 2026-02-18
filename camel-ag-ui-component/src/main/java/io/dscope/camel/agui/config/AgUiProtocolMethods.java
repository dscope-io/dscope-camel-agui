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

package io.dscope.camel.agui.config;

import java.util.Set;

public final class AgUiProtocolMethods {

    public static final String RUN_START = "run.start";
    public static final String RUN_TEXT = "run.text";
    public static final String TOOL_CALL = "tool.call";
    public static final String STATE_UPDATE = "state.update";
    public static final String INTERRUPT = "run.interrupt";
    public static final String RESUME = "run.resume";
    public static final String RUN_FINISH = "run.finish";
    public static final String HEALTH = "health";

    public static final Set<String> CORE_METHODS = Set.of(
        RUN_START,
        RUN_TEXT,
        TOOL_CALL,
        STATE_UPDATE,
        INTERRUPT,
        RESUME,
        RUN_FINISH,
        HEALTH
    );

    private AgUiProtocolMethods() {
    }
}
