package com.tpi.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrdenResponse(
        UUID orderId,
        String status,
        BigDecimal matchedQuantity,
        BigDecimal remainingQuantity,
        BigDecimal executionPriceArs,
        String message
) {
}

