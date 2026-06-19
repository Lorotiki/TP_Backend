package com.tpi.orders.dto;

import java.math.BigDecimal;

public record PortfolioClientPositionResponse(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPriceArs
) {
}

