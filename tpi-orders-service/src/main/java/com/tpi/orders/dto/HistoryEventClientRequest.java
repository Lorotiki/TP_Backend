package com.tpi.orders.dto;

import java.util.Map;
import java.util.UUID;

public record HistoryEventClientRequest(
        UUID eventId,
        String eventType,
        String userId,
        UUID orderId,
        UUID correlationId,
        UUID causationId,
        Map<String, Object> payloadJson
) {
}

