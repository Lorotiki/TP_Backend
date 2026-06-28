package com.tpi.portfolio.dto;

import java.util.Map;
import java.util.UUID;

public record HistorialEventoClienteRequest(
        UUID eventId,
        String eventType,
        String userId,
        UUID orderId,
        UUID correlationId,
        UUID causationId,
        Map<String, Object> payloadJson
) {
}
