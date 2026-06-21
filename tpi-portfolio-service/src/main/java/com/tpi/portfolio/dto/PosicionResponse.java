package com.tpi.portfolio.dto;

import java.math.BigDecimal;

public record PosicionResponse(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPriceArs
) {
}

