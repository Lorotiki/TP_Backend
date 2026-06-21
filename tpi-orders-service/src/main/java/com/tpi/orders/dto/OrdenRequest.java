package com.tpi.orders.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrdenRequest(
        String userId,
        @NotBlank String simbolo,
        @NotBlank String simboloMoneda,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal cantidad,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precioLimite,
        String lado
) {
}

