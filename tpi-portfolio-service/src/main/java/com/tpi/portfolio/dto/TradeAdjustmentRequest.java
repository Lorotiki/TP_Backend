package com.tpi.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradeAdjustmentRequest(
        @NotBlank String side,
        @NotBlank String symbol,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.01") BigDecimal priceArs,
        @NotBlank String referenceId
) {
}

