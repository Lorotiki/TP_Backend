package com.tpi.orders.dto;

import java.math.BigDecimal;

public record QuoteClientResponse(
        String symbol,
        BigDecimal price,
        String currency,
        String source,
        String updatedAt
) {
}

