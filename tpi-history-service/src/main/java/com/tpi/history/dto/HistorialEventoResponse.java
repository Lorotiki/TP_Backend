package com.tpi.history.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record HistorialEventoResponse(
        UUID eventId,
        String eventType,
        String userId,
        UUID orderId,
        UUID correlationId,
        UUID causationId,
        Map<String, Object> payloadJson,
        LocalDateTime occurredAt
) {
}

