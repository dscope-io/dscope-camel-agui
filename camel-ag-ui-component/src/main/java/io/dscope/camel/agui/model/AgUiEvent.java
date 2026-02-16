package io.dscope.camel.agui.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgUiEvent {

    private String type;
    private Instant timestamp;
    private String runId;
    private String sessionId;
    private Map<String, Object> metadata;

    public AgUiEvent() {
        this.timestamp = Instant.now();
    }

    public AgUiEvent(String type, String runId, String sessionId) {
        this.type = type;
        this.runId = runId;
        this.sessionId = sessionId;
        this.timestamp = Instant.now();
    }

    public AgUiEvent with(String key, Object value) {
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
        metadata.put(key, value);
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
