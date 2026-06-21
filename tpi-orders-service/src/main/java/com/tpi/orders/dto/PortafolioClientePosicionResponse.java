package com.tpi.orders.dto;

import java.math.BigDecimal;

public record PortafolioClientePosicionResponse(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPriceArs
) {
}

