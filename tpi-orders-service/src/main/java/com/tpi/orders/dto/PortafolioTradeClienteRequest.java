package com.tpi.orders.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PortafolioTradeClienteRequest(
        @JsonProperty("side")
        @JsonAlias("lado")
        String lado,
        @JsonProperty("symbol")
        @JsonAlias("simbolo")
        String simbolo,
        @JsonProperty("quantity")
        @JsonAlias("cantidad")
        BigDecimal cantidad,
        @JsonProperty("priceArs")
        @JsonAlias("precioLimite")
        BigDecimal precioLimite,
        @JsonProperty("referenceId")
        @JsonAlias("referenciaId")
        String referenciaId
) {
}

