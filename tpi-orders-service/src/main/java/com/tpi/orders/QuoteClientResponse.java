package com.tpi.orders;

import java.math.BigDecimal;

public record QuoteClientResponse(
        String symbol,
        BigDecimal price,
        String currency,
        String source,
        String updatedAt
) {
}

