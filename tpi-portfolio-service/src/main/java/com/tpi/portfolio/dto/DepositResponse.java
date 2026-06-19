package com.tpi.portfolio.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositResponse(
        UUID movementId,
        BigDecimal balanceArs
) {
}

