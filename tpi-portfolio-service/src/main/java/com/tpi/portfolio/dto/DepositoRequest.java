package com.tpi.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositoRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountArs,
        @NotBlank String referenceId
) {
}

