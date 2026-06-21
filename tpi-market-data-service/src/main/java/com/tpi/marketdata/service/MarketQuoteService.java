package com.tpi.marketdata.service;

import com.tpi.marketdata.dto.QuoteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MarketQuoteService {

    private final RestTemplate restTemplate;

    public MarketQuoteService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("MarketQuoteService inicializado con RestTemplate");
    }

    public QuoteResponse getQuote(String symbol) {
        var normalized = normalize(symbol);
        log.debug("Obteniendo cotización para símbolo: {}", normalized);
        
        try {
            return getQuoteFromYahooFinance(normalized);
        } catch (Exception e) {
            log.error("Error al obtener cotización de Yahoo Finance para {}: {}", normalized, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                "No se pudo obtener la cotización para " + normalized);
        }
    }

    private QuoteResponse getQuoteFromYahooFinance(String symbol) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol;
        log.debug("Consultando Yahoo Finance: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> respuesta = restTemplate.exchange(
            url, HttpMethod.GET, entity, Map.class
        );
        
        if (respuesta.getBody() == null) {
            log.warn("Respuesta vacía de Yahoo Finance para {}", symbol);
            throw new IllegalArgumentException("Respuesta vacía del servidor");
        }
        
        Map<String, Object> chart = (Map<String, Object>) respuesta.getBody().get("chart");
        List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");
        Map<String, Object> datos = result.get(0);
        Map<String, Object> meta = (Map<String, Object>) datos.get("meta");
        
        String nombre = (String) meta.get("shortName");
        Double precioUSD = ((Number) meta.get("regularMarketPrice")).doubleValue();
        String moneda = (String) meta.get("currency");
        
        log.info("Cotización obtenida: {} = {} {}", symbol, precioUSD, moneda);
        
        BigDecimal precioARS = convertToARS(new BigDecimal(precioUSD), moneda);
        
        return new QuoteResponse(symbol, precioARS, "ARS", "YAHOO_FINANCE", OffsetDateTime.now());
    }

    private BigDecimal convertToARS(BigDecimal precio, String moneda) {
        if ("ARS".equalsIgnoreCase(moneda)) {
            log.debug("Precio ya está en ARS");
            return precio;
        }
        
        if ("USD".equalsIgnoreCase(moneda)) {
            BigDecimal tasa = new BigDecimal("800.00");
            BigDecimal resultado = precio.multiply(tasa);
            log.debug("Conversión USD->ARS: {} USD = {} ARS", precio, resultado);
            return resultado;
        }
        
        log.warn("Moneda desconocida: {}, asumiendo USD", moneda);
        return precio.multiply(new BigDecimal("800.00"));
    }

    private String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El símbolo es obligatorio");
        }
        return symbol.trim().toUpperCase();
    }
}

