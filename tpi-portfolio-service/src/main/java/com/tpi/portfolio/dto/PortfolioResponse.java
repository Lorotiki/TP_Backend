package com.tpi.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        String userId,
        BigDecimal balanceArs,
        List<PositionResponse> positions
) {
}

