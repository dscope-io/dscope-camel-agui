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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.*;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** The offline schema boundary. Strict emission and tolerant reception use the same definitions. */
public final class AgUiSchema {
  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final JsonNode DOCUMENT = load();
  private static final Map<String, JsonSchema> VALIDATORS = new ConcurrentHashMap<>();

  public static boolean validationEnabled() {
    String value = System.getProperty("agui.validation.enabled", "true").trim();
    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
      throw new IllegalArgumentException("agui.validation.enabled must be true or false");
    return Boolean.parseBoolean(value);
  }

  private static JsonNode load() {
    try (InputStream stream =
        AgUiSchema.class.getResourceAsStream("/io/dscope/camel/agui/schema-1.0.json")) {
      if (stream == null) throw new IllegalStateException("Missing AG-UI schema");
      return MAPPER.readTree(stream);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load AG-UI schema", e);
    }
  }

  public static JsonNode definition(String name) {
    return DOCUMENT.path("$defs").path(name);
  }

  public static void validate(String name, JsonNode value, boolean enabled) {
    if (!enabled) return;
    JsonSchema schema =
        VALIDATORS.computeIfAbsent(
            name,
            key -> {
              ObjectNode root = MAPPER.createObjectNode();
              root.put("$schema", DOCUMENT.path("$schema").asText());
              root.put("$ref", "#/$defs/" + key);
              root.set("$defs", DOCUMENT.path("$defs"));
              return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(root);
            });
    var errors = schema.validate(value);
    if (!errors.isEmpty())
      throw new IllegalArgumentException("Invalid AG-UI " + name + ": " + errors);
  }

  public static JsonNode receive(
      String name, JsonNode value, boolean validate, Consumer<String> warning) {
    JsonNode clean = clean(definition(name), value.deepCopy(), "$", warning);
    if (!clean.isMissingNode()) validate(name, clean, validate);
    return clean;
  }

  private static JsonNode resolve(JsonNode schema) {
    return schema.has("$ref")
        ? resolve(definition(schema.get("$ref").asText().substring("#/$defs/".length())))
        : schema;
  }

  private static ObjectNode properties(JsonNode schema) {
    ObjectNode out = MAPPER.createObjectNode();
    for (JsonNode base : schema.path("allOf")) out.setAll(properties(resolve(base)));
    if (schema.path("properties").isObject()) out.setAll((ObjectNode) schema.get("properties"));
    return out;
  }

  private static boolean closed(JsonNode schema) {
    return schema.path("unevaluatedProperties").isBoolean()
            && !schema.get("unevaluatedProperties").asBoolean()
        || schema.path("additionalProperties").isBoolean()
            && !schema.get("additionalProperties").asBoolean();
  }

  private static JsonNode drop(String path, Consumer<String> warning) {
    warning.accept("Dropping unrecognised AG-UI material at " + path);
    return MissingNode.getInstance();
  }

  private static JsonNode clean(
      JsonNode source, JsonNode value, String path, Consumer<String> warning) {
    JsonNode schema = resolve(source);
    if (schema.has("oneOf")) {
      List<JsonNode> variants = new ArrayList<>();
      schema.get("oneOf").forEach(v -> variants.add(resolve(v)));
      if (value.isObject()) {
        for (String tag : List.of("type", "role", "op")) {
          List<JsonNode> tagged =
              variants.stream().filter(v -> properties(v).path(tag).has("const")).toList();
          if (!tagged.isEmpty() && value.has(tag) && value.get(tag).isTextual()) {
            for (JsonNode v : tagged)
              if (properties(v).path(tag).get("const").equals(value.get(tag)))
                return clean(v, value, path, warning);
            return drop(path, warning);
          }
        }
      }
      for (JsonNode v : variants) {
        String type = v.path("type").asText();
        if (type.equals("string") && value.isTextual()
            || type.equals("array") && value.isArray()
            || type.equals("object") && value.isObject()) return clean(v, value, path, warning);
      }
      return value; // Known malformed values remain for strict validation.
    }
    if (schema.has("enum") && value.isTextual()) {
      boolean known = false;
      for (JsonNode v : schema.get("enum")) if (v.equals(value)) known = true;
      if (!known) return drop(path, warning);
    }
    if (value.isArray() && schema.has("items")) {
      ArrayNode result = MAPPER.createArrayNode();
      int i = 0;
      for (JsonNode item : value) {
        JsonNode c = clean(schema.get("items"), item, path + "[" + i++ + "]", warning);
        if (!c.isMissingNode()) result.add(c);
      }
      return result;
    }
    if (value.isObject()) {
      ObjectNode p = properties(schema);
      Set<String> required = new HashSet<>();
      schema.path("required").forEach(v -> required.add(v.asText()));
      ObjectNode result = (ObjectNode) value;
      List<String> keys = new ArrayList<>();
      result.fieldNames().forEachRemaining(keys::add);
      for (String key : keys) {
        if (p.has(key)) {
          JsonNode c = clean(p.get(key), result.get(key), path + "." + key, warning);
          if (c.isMissingNode()) {
            if (required.contains(key)) return drop(path, warning);
            result.remove(key);
          } else result.set(key, c);
        } else if (closed(schema)) {
          warning.accept("Stripping unknown AG-UI field " + path + "." + key);
          result.remove(key);
        }
      }
    }
    return value;
  }

  private AgUiSchema() {}
}
