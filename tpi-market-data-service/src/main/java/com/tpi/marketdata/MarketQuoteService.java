package com.tpi.marketdata;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class MarketQuoteService {

    private static final Map<String, BigDecimal> QUOTES = Map.of(
            "AAPL", new BigDecimal("27500.00"),
            "AMZN", new BigDecimal("18250.00"),
            "GOOGL", new BigDecimal("30110.00"),
            "MELI", new BigDecimal("65400.00"),
            "MSFT", new BigDecimal("31200.00"),
            "NFLX", new BigDecimal("28990.00"),
            "NVDA", new BigDecimal("36000.00"),
            "TSLA", new BigDecimal("24150.00")
    );

    public QuoteResponse getQuote(String symbol) {
        var normalized = normalize(symbol);
        var price = QUOTES.get(normalized);
        if (price == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe cotización para el símbolo " + normalized);
        }
        return new QuoteResponse(normalized, price, "ARS", "STATIC_CATALOG", OffsetDateTime.now());
    }

    private String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El símbolo es obligatorio");
        }
        return symbol.trim().toUpperCase();
    }
}

