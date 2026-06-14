package com.tpi.orders;

import java.math.BigDecimal;

public record PortfolioClientPositionResponse(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPriceArs
) {
}

