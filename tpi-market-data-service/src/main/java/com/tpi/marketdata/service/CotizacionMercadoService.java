package com.tpi.marketdata.service;

import com.tpi.marketdata.dto.CotizacionResponse;
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
public class CotizacionMercadoService {

    private final RestTemplate restTemplate;

    public CotizacionMercadoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("CotizacionMercadoService inicializado con RestTemplate");
    }

    public CotizacionResponse getCotizacion(String simbolo) {
        var simboloNormalizado = normalizarSimbolo(simbolo);
        log.debug("Obteniendo cotización para símbolo: {}", simboloNormalizado);
        
        try {
            return getCotizacionDesdeYahooFinance(simboloNormalizado);
        } catch (Exception e) {
            log.error("Error al obtener cotización de Yahoo Finance para {}: {}", simboloNormalizado, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                "No se pudo obtener la cotización para " + simboloNormalizado);
        }
    }

    private CotizacionResponse getCotizacionDesdeYahooFinance(String simbolo) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + simbolo;
        log.debug("Consultando Yahoo Finance: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        
        HttpEntity<String> entidad = new HttpEntity<>(headers);
        
        ResponseEntity<Map> respuesta = restTemplate.exchange(
            url, HttpMethod.GET, entidad, Map.class
        );
        
        if (respuesta.getBody() == null) {
            log.warn("Respuesta vacía de Yahoo Finance para {}", simbolo);
            throw new IllegalArgumentException("Respuesta vacía del servidor");
        }
        
        Map<String, Object> caracter = (Map<String, Object>) respuesta.getBody().get("chart");
        List<Map<String, Object>> resultado = (List<Map<String, Object>>) caracter.get("result");
        Map<String, Object> datos = resultado.get(0);
        Map<String, Object> meta = (Map<String, Object>) datos.get("meta");
        
        String nombre = (String) meta.get("shortName");
        Double precioUSD = ((Number) meta.get("regularMarketPrice")).doubleValue();
        String moneda = (String) meta.get("currency");
        
        log.info("Cotización obtenida: {} = {} {}", simbolo, precioUSD, moneda);
        
        BigDecimal precioARS = convertirMonedaAPesosArg(new BigDecimal(precioUSD), moneda);
        
        return new CotizacionResponse(simbolo, precioARS, "ARS", "YAHOO_FINANCE", OffsetDateTime.now());
    }

    private BigDecimal convertirMonedaAPesosArg(BigDecimal precio, String moneda) {
        if ("ARS".equalsIgnoreCase(moneda)) {
            log.debug("Precio ya está en ARS");
            return precio;
        }

        //to-do armar para levantar la cotizacion oficial???
        // aca se modificar la cotizacion del dolar
        if ("USD".equalsIgnoreCase(moneda)) {
            BigDecimal tasa = new BigDecimal("800.00");
            BigDecimal resultado = precio.multiply(tasa);
            log.debug("Conversión USD->ARS: {} USD = {} ARS", precio, resultado);
            return resultado;
        }
        
        log.warn("Moneda desconocida: {}, asumiendo USD", moneda);
        return precio.multiply(new BigDecimal("800.00"));
    }

    private String normalizarSimbolo(String simbolo) {
        if (simbolo == null || simbolo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El símbolo es obligatorio");
        }
        return simbolo.trim().toUpperCase();
    }
}

