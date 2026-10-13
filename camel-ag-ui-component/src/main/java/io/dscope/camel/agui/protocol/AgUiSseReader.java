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

package io.dscope.camel.agui.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** SSE framing with UTF-8 boundaries handled by the reader, independent of protocol enforcement. */
public final class AgUiSseReader {
  public static void read(InputStream stream, Consumer<JsonNode> event) throws IOException {
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)));
    StringBuilder data = new StringBuilder();
    String line;
    boolean first = true, hasData = false;
    while ((line = reader.readLine()) != null) {
      if (first) {
        first = false;
        if (line.startsWith("\uFEFF")) line = line.substring(1);
      }
      if (line.isEmpty()) {
        if (hasData) {
          JsonNode parsed = AgUiSchema.MAPPER.readTree(data.toString());
          if (parsed == null) throw new IOException("Empty AG-UI SSE data");
          event.accept(parsed);
          data.setLength(0);
          hasData = false;
        }
        continue;
      }
      if (line.startsWith(":")) continue;
      int colon = line.indexOf(':');
      String field = colon < 0 ? line : line.substring(0, colon);
      if (!field.equals("data")) continue;
      String value = colon < 0 ? "" : line.substring(colon + 1);
      if (value.startsWith(" ")) value = value.substring(1);
      if (hasData) data.append('\n');
      data.append(value);
      hasData = true;
    }
    // SSE dispatches only at an empty line; an incomplete final frame is not delivered.
    if (hasData) throw new EOFException("Incomplete SSE frame");
  }

  private AgUiSseReader() {}
}
