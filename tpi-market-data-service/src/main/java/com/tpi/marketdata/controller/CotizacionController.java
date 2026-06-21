package com.tpi.marketdata.controller;

import com.tpi.marketdata.dto.CotizacionResponse;

import com.tpi.marketdata.service.CotizacionMercadoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotes")
public class CotizacionController {

    private final CotizacionMercadoService cotizacionMercadoService;

    public CotizacionController(CotizacionMercadoService cotizacionMercadoService) {
        this.cotizacionMercadoService = cotizacionMercadoService;
    }

    @GetMapping("/{symbol}")
    public CotizacionResponse getQuote(@PathVariable String symbol) {
        return cotizacionMercadoService.getCotizacion(symbol);
    }
}

