package com.tpi.orders.dto;

import java.math.BigDecimal;

public record PortafolioTradeClienteRequest(
        String lado,
        String simbolo,
        BigDecimal cantidad,
        BigDecimal precioLimite,
        String referenciaId
) {
}

