package com.tpi.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortafolioResponse(
        String userId,
        BigDecimal balanceArs,
        List<PosicionResponse> positions
) {
}

