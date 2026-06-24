package com.tpi.orders.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record PortafolioClienteResponse(
        String userId,
        @JsonProperty("balanceArs")
        @JsonAlias("valorTotalArs")
        BigDecimal valorTotalArs,
        @JsonProperty("positions")
        @JsonAlias("posiciones")
        List<PortafolioClientePosicionResponse> posiciones
) {
}

