package com.tpi.portfolio;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositResponse(
        UUID movementId,
        BigDecimal balanceArs
) {
}

