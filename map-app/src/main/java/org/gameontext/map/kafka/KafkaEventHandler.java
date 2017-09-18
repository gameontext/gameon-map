package org.gameontext.map.kafka;

import com.fasterxml.jackson.databind.JsonNode;

public interface KafkaEventHandler {
    String getEventType();

    void handleEvent(String key, JsonNode eventData);
}
