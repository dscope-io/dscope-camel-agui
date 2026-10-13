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

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;

@org.apache.camel.spi.UriParams
public class AgUiConfiguration {

  @UriPath(description = "Agent identifier for this endpoint")
  @Metadata(required = true)
  private String agentId;

  @UriParam(
      label = "consumer",
      defaultValue = "undertow:http://0.0.0.0:8080",
      description = "Undertow listener URL for incoming agent requests")
  private String serverUrl = "undertow:http://0.0.0.0:8080";

  @UriParam(
      label = "producer",
      defaultValue = "http://localhost:8080",
      description = "Base URL of the remote AG-UI server")
  private String remoteUrl = "http://localhost:8080";

  @UriParam(
      label = "consumer,producer",
      defaultValue = "/agui/rpc",
      description = "Legacy JSON-RPC request path")
  private String rpcPath = "/agui/rpc";

  @UriParam(
      label = "consumer,producer",
      defaultValue = "/agui/stream",
      description = "Legacy event replay path")
  private String streamPath = "/agui/stream";

  @UriParam(
      label = "consumer",
      defaultValue = "*",
      description = "Allowed origins for the legacy WebSocket binding")
  private String allowedOrigins = "*";

  @UriParam(
      label = "consumer",
      defaultValue = "false",
      description = "Broadcast legacy WebSocket responses")
  private boolean sendToAll = false;

  @UriParam(
      label = "consumer",
      defaultValue = "false",
      description = "Enable WebSocket consumer scaffolding route in addition to HTTP RPC route.")
  private boolean websocketEnabled = false;

  @UriParam(
      label = "consumer",
      defaultValue = "/agui/ws",
      description = "WebSocket path used when websocketEnabled=true.")
  private String wsPath = "/agui/ws";

  @UriParam(label = "producer", defaultValue = "run.start", description = "Legacy JSON-RPC method")
  private String method = "run.start";

  @UriParam(
      label = "consumer,producer",
      defaultValue = "agui/2026-01-01",
      description = "Legacy configured protocol identifier; standard HTTP/SSE declares 1.0")
  private String protocolVersion = "agui/2026-01-01";

  @UriParam(
      label = "consumer,producer",
      defaultValue = "jsonRpc",
      description = "Choose legacy jsonRpc or standard httpSse transport")
  private String transportMode = "jsonRpc";

  @UriParam(
      label = "consumer,producer",
      defaultValue = "/agui/agent",
      description = "Standard HTTP/SSE agent endpoint path")
  private String agentPath = "/agui/agent";

  @UriParam(
      label = "producer",
      description = "Camel endpoint receiving each normalized HTTP/SSE event in order")
  private String eventsEndpoint;

  public String getTransportMode() {
    return transportMode;
  }

  public void setTransportMode(String value) {
    if (!"jsonRpc".equals(value) && !"httpSse".equals(value))
      throw new IllegalArgumentException("transportMode must be jsonRpc or httpSse");
    transportMode = value;
  }

  public String getAgentPath() {
    return agentPath;
  }

  public void setAgentPath(String value) {
    agentPath = value;
  }

  public String getEventsEndpoint() {
    return eventsEndpoint;
  }

  public void setEventsEndpoint(String value) {
    eventsEndpoint = value;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getRemoteUrl() {
    return remoteUrl;
  }

  public void setRemoteUrl(String remoteUrl) {
    this.remoteUrl = remoteUrl;
  }

  public String getRpcPath() {
    return rpcPath;
  }

  public void setRpcPath(String rpcPath) {
    this.rpcPath = rpcPath;
  }

  public String getStreamPath() {
    return streamPath;
  }

  public void setStreamPath(String streamPath) {
    this.streamPath = streamPath;
  }

  public String getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(String allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  public boolean isSendToAll() {
    return sendToAll;
  }

  public void setSendToAll(boolean sendToAll) {
    this.sendToAll = sendToAll;
  }

  public boolean isWebsocketEnabled() {
    return websocketEnabled;
  }

  public void setWebsocketEnabled(boolean websocketEnabled) {
    this.websocketEnabled = websocketEnabled;
  }

  public String getWsPath() {
    return wsPath;
  }

  public void setWsPath(String wsPath) {
    this.wsPath = wsPath;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
  }
}
