package com.tpi.orders.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioClientResponse(
        String userId,
        BigDecimal balanceArs,
        List<PortfolioClientPositionResponse> positions
) {
}

