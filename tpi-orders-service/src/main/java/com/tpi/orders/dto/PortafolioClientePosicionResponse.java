package com.tpi.orders.dto;

import java.math.BigDecimal;

public record PortafolioClientePosicionResponse(
        String simbolo,
        BigDecimal cantidad,
        BigDecimal precioPromedioArs
) {
}

