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
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** RFC 6902 application, retaining exact JSON values and rejecting invalid paths/tests. */
public final class AgUiJsonPatch {
  public static JsonNode apply(JsonNode original, JsonNode operations) {
    JsonNode root = original.deepCopy();
    for (JsonNode op : operations) {
      if (!op.path("op").isTextual() || !op.path("path").isTextual())
        throw new IllegalArgumentException("Patch requires op and path strings");
      String kind = op.path("op").asText(), path = op.path("path").asText();
      switch (kind) {
        case "test" -> {
          if (!read(root, path)
              .equals(
                  (a, b) ->
                      a.isNumber() && b.isNumber()
                          ? a.decimalValue().compareTo(b.decimalValue())
                          : a.equals(b) ? 0 : 1,
                  op.get("value"))) throw new IllegalArgumentException("JSON Patch test failed");
        }
        case "copy", "move" -> {
          String from = op.path("from").asText();
          JsonNode value = read(root, from).deepCopy();
          if (kind.equals("move")) {
            if (path.startsWith(from + "/"))
              throw new IllegalArgumentException("Cannot move a value into itself");
            root = change(root, from, null, "remove");
          }
          root = change(root, path, value, "add");
        }
        case "add", "replace", "remove" -> root = change(root, path, op.get("value"), kind);
        default -> throw new IllegalArgumentException("Unknown JSON Patch operation");
      }
    }
    return root;
  }

  private static List<String> tokens(String path) {
    if (path.isEmpty()) return List.of();
    if (!path.startsWith("/") || path.matches(".*~(?![01]).*"))
      throw new IllegalArgumentException("Invalid JSON pointer");
    return Arrays.stream(path.substring(1).split("/", -1))
        .map(s -> s.replace("~1", "/").replace("~0", "~"))
        .toList();
  }

  private static int index(String token, int size, boolean add) {
    if (add && token.equals("-")) return size;
    if (!token.matches("0|[1-9][0-9]*")) throw new IllegalArgumentException("Invalid array index");
    int i = Integer.parseInt(token);
    if (i < 0 || i >= size + (add ? 1 : 0))
      throw new IllegalArgumentException("Array index out of bounds");
    return i;
  }

  private static JsonNode child(JsonNode node, String key) {
    JsonNode result =
        node.isArray()
            ? node.get(index(key, node.size(), false))
            : node.isObject() ? node.get(key) : null;
    if (result == null) throw new IllegalArgumentException("JSON pointer does not exist");
    return result;
  }

  private static JsonNode read(JsonNode root, String path) {
    for (String key : tokens(path)) root = child(root, key);
    return root;
  }

  private static JsonNode change(JsonNode root, String path, JsonNode value, String kind) {
    List<String> keys = tokens(path);
    if (keys.isEmpty()) {
      if (kind.equals("remove")) return NullNode.getInstance();
      if (value == null) throw new IllegalArgumentException("Missing patch value");
      return value.deepCopy();
    }
    JsonNode parent = root;
    for (int i = 0; i < keys.size() - 1; i++) parent = child(parent, keys.get(i));
    String key = keys.get(keys.size() - 1);
    if (!kind.equals("add")) child(parent, key);
    if (parent.isObject()) {
      if (kind.equals("remove")) ((ObjectNode) parent).remove(key);
      else {
        if (value == null) throw new IllegalArgumentException("Missing patch value");
        ((ObjectNode) parent).set(key, value.deepCopy());
      }
    } else if (parent.isArray()) {
      if (!kind.equals("remove") && value == null)
        throw new IllegalArgumentException("Missing patch value");
      ArrayNode a = (ArrayNode) parent;
      int i = index(key, a.size(), kind.equals("add"));
      if (kind.equals("remove")) a.remove(i);
      else if (kind.equals("add")) a.insert(i, value.deepCopy());
      else a.set(i, value.deepCopy());
    } else throw new IllegalArgumentException("Patch parent is not a container");
    return root;
  }

  private AgUiJsonPatch() {}
}
