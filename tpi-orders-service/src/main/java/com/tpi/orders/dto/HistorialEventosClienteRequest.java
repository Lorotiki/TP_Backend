package com.tpi.orders.dto;

import java.util.Map;
import java.util.UUID;

public record HistorialEventosClienteRequest(
        Integer eventoId,
        String tipoEvento,
        String userId,
        Integer ordenId,
        Integer correlacionId,
        Integer referenciaId,
        Map<String, Object> payloadJson
) {
}

