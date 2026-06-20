package com.tpi.history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/*
UUID Universall Unique Identifier
@NotBlack Validacion de Bean Validation para controlar que el texto no venga vacio o con espacios en blanco
@NotNull Validacion de Bean Validation para controlar que el valor no sea nulo
 */

public record HistoryEventRequest(
        UUID eventId,
        @NotBlank String eventType,
        String userId,
        UUID orderId,
        UUID correlationId,
        UUID causationId,
        @NotNull Map<String, Object> payloadJson
) {
}

