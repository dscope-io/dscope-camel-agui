package io.dscope.camel.agui.service;

public record AgUiSessionEventRecord(long sequence, String eventType, String json) {
}
