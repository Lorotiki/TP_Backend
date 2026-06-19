package com.tpi.orders.dto;

import java.math.BigDecimal;

public record PortfolioTradeClientRequest(
        String side,
        String symbol,
        BigDecimal quantity,
        BigDecimal priceArs,
        String referenceId
) {
}

