package com.tpi.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrdenResponse(
        Integer ordenId,
        String estado,
        BigDecimal cantidad,
        BigDecimal cantidadRestante,
        BigDecimal precioLimite,
        String mensaje
) {
}

