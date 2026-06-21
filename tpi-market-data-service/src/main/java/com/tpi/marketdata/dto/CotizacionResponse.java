package com.tpi.marketdata.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CotizacionResponse(
        String symbol,
        BigDecimal price,
        String currency,
        String source,
        OffsetDateTime updatedAt
) {
}

