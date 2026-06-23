package com.tpi.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrdenRequest(
        @NotBlank(message = "El ID de usuario (userId) no puede estar vacío.")
        String userId,

        @NotBlank(message = "El símbolo (symbol) no puede estar vacío.")
        String symbol,

        @NotBlank(message = "El lado de la orden (side) no puede estar vacío.")
        @Pattern(regexp = "BUY|SELL", message = "El lado de la orden (side) debe ser 'BUY' o 'SELL'.")
        String side,

        @NotNull(message = "La cantidad (quantity) no puede ser nula.")
        @Positive(message = "La cantidad (quantity) debe ser un número positivo.")
        BigDecimal quantity,

        @NotNull(message = "El precio límite (limitPrice) no puede ser nulo.")
        @Positive(message = "El precio límite (limitPrice) debe ser un número positivo.")
        BigDecimal limitPrice
) {}