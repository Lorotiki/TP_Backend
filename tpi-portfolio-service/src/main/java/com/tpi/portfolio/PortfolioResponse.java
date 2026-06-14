package com.tpi.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        String userId,
        BigDecimal balanceArs,
        List<PositionResponse> positions
) {
}

