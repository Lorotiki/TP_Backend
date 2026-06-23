package com.tpi.portfolio.dto;

import java.math.BigDecimal;
import java.util.UUID;


public record DepositoResponse(
        UUID movementId,
        BigDecimal balanceArs
) {}
