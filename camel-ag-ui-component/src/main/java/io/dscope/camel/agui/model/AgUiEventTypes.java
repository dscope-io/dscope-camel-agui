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

public final class AgUiEventTypes {

  public static final String RUN_STARTED = "RUN_STARTED";
  public static final String RUN_FINISHED = "RUN_FINISHED";
  public static final String RUN_ERROR = "RUN_ERROR";
  public static final String STEP_STARTED = "STEP_STARTED";
  public static final String STEP_FINISHED = "STEP_FINISHED";
  public static final String TEXT_MESSAGE_START = "TEXT_MESSAGE_START";
  public static final String TEXT_MESSAGE_CONTENT = "TEXT_MESSAGE_CONTENT";
  public static final String TEXT_MESSAGE_END = "TEXT_MESSAGE_END";
  public static final String TOOL_CALL_START = "TOOL_CALL_START";
  public static final String TOOL_CALL_ARGS = "TOOL_CALL_ARGS";
  public static final String TOOL_CALL_END = "TOOL_CALL_END";
  public static final String TOOL_CALL_RESULT = "TOOL_CALL_RESULT";
  public static final String STATE_SNAPSHOT = "STATE_SNAPSHOT";
  public static final String STATE_DELTA = "STATE_DELTA";
  public static final String CUSTOM = "CUSTOM";
  public static final String INTERRUPT_REQUESTED = "INTERRUPT_REQUESTED";
  public static final String RESUME_ACCEPTED = "RESUME_ACCEPTED";

  public static final String TEXT_MESSAGE_CHUNK = "TEXT_MESSAGE_CHUNK";

  public static final String TOOL_CALL_CHUNK = "TOOL_CALL_CHUNK";

  public static final String MESSAGES_SNAPSHOT = "MESSAGES_SNAPSHOT";

  public static final String ACTIVITY_SNAPSHOT = "ACTIVITY_SNAPSHOT";

  public static final String ACTIVITY_DELTA = "ACTIVITY_DELTA";

  public static final String RAW = "RAW";

  public static final String REASONING_START = "REASONING_START";

  public static final String REASONING_MESSAGE_START = "REASONING_MESSAGE_START";

  public static final String REASONING_MESSAGE_CONTENT = "REASONING_MESSAGE_CONTENT";

  public static final String REASONING_MESSAGE_END = "REASONING_MESSAGE_END";

  public static final String REASONING_MESSAGE_CHUNK = "REASONING_MESSAGE_CHUNK";

  public static final String REASONING_END = "REASONING_END";

  public static final String REASONING_ENCRYPTED_VALUE = "REASONING_ENCRYPTED_VALUE";

  public static final String SUBAGENT_STARTED = "SUBAGENT_STARTED";

  public static final String SUBAGENT_FINISHED = "SUBAGENT_FINISHED";

  public static final String SUBAGENT_ERROR = "SUBAGENT_ERROR";

  private AgUiEventTypes() {}
}
