package io.dscope.camel.agui;

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;

public class AgUiConfiguration {

    @UriPath(description = "Agent identifier for this endpoint")
    @Metadata(required = true)
    private String agentId;

    @UriParam(label = "consumer", defaultValue = "undertow:http://0.0.0.0:8081")
    private String serverUrl = "undertow:http://0.0.0.0:8081";

    @UriParam(label = "producer", defaultValue = "http://localhost:8081")
    private String remoteUrl = "http://localhost:8081";

    @UriParam(label = "consumer,producer", defaultValue = "/agui/rpc")
    private String rpcPath = "/agui/rpc";

    @UriParam(label = "consumer,producer", defaultValue = "/agui/stream")
    private String streamPath = "/agui/stream";

    @UriParam(label = "consumer", defaultValue = "*")
    private String allowedOrigins = "*";

    @UriParam(label = "consumer", defaultValue = "false")
    private boolean sendToAll = false;

    @UriParam(label = "consumer", defaultValue = "false",
        description = "Enable WebSocket consumer scaffolding route in addition to HTTP RPC route.")
    private boolean websocketEnabled = false;

    @UriParam(label = "consumer", defaultValue = "/agui/ws",
        description = "WebSocket path used when websocketEnabled=true.")
    private String wsPath = "/agui/ws";

    @UriParam(label = "producer", defaultValue = "run.start")
    private String method = "run.start";

    @UriParam(label = "consumer,producer", defaultValue = "agui/2026-01-01")
    private String protocolVersion = "agui/2026-01-01";

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
