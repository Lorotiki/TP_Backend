package com.tpi.marketdata.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CotizacionResponse(
        String simbolo,
        BigDecimal precio,
        String compañia,
        String moneda,
        OffsetDateTime marcaTiempo
) {
}

