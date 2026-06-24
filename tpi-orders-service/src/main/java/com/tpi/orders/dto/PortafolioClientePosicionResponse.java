package com.tpi.orders.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PortafolioClientePosicionResponse(
        @JsonProperty("symbol")
        @JsonAlias("simbolo")
        String simbolo,
        @JsonProperty("quantity")
        @JsonAlias("cantidad")
        BigDecimal cantidad,
        @JsonProperty("avgPriceArs")
        @JsonAlias("precioPromedioArs")
        BigDecimal precioPromedioArs
) {
}

