package com.tpi.orders.dto;

import java.math.BigDecimal;

public record CotizacionClienteResponse(
        String symbol,
        BigDecimal price,
        String currency,
        String source,
        String updatedAt
) {
}

