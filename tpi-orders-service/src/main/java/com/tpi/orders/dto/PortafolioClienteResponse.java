package com.tpi.orders.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortafolioClienteResponse(
        String userId,
        BigDecimal balanceArs,
        List<PortafolioClientePosicionResponse> positions
) {
}

