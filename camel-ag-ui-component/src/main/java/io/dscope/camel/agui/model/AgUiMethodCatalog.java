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

package io.dscope.camel.agui.model;

import io.dscope.camel.agui.config.AgUiProtocolMethods;
import java.util.Map;

public class AgUiMethodCatalog {

    public Map<String, String> supportedMethods() {
        return Map.of(
            AgUiProtocolMethods.RUN_START, "Start run and open stream session",
            AgUiProtocolMethods.RUN_TEXT, "Emit streaming text chunks",
            AgUiProtocolMethods.TOOL_CALL, "Emit tool call lifecycle",
            AgUiProtocolMethods.STATE_UPDATE, "Update and stream mutable state",
            AgUiProtocolMethods.INTERRUPT, "Request human-in-the-loop interrupt",
            AgUiProtocolMethods.RESUME, "Resume interrupted run",
            AgUiProtocolMethods.RUN_FINISH, "Complete run",
            AgUiProtocolMethods.HEALTH, "Runtime health check"
        );
    }
}
