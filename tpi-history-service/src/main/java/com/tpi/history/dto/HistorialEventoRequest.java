package com.tpi.history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;


public record HistorialEventoRequest(
        UUID eventId,
        @NotBlank String eventType,
        String userId,
        UUID orderId,
        UUID correlationId,
        UUID causationId,
        @NotNull Map<String, Object> payloadJson
) {
}

