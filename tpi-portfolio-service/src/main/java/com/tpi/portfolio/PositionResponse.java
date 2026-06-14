package com.tpi.portfolio;

import java.math.BigDecimal;

public record PositionResponse(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPriceArs
) {
}

