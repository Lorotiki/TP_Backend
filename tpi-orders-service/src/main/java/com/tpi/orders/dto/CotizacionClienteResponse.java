package com.tpi.orders.dto;

import java.math.BigDecimal;

public record CotizacionClienteResponse(
        String simbolo,
        BigDecimal precio,
        String moneda,
        String fuente,
        String actualizadoEn
) {
}

