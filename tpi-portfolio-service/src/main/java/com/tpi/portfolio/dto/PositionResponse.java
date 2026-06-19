package com.tpi.portfolio.dto;

import java.math.BigDecimal;

public record PositionResponse(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPriceArs
) {
}

