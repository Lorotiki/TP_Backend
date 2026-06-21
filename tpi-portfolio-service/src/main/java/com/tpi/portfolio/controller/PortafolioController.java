package com.tpi.portfolio.controller;

import com.tpi.portfolio.dto.DepositoRequest;
import com.tpi.portfolio.dto.DepositoResponse;
import com.tpi.portfolio.dto.PortafolioResponse;
import com.tpi.portfolio.dto.TradeAdjustmentRequest;
import com.tpi.portfolio.service.PortafolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}")
public class PortafolioController {

    private final PortafolioService portafolioService;

    public PortafolioController(PortafolioService portafolioService) {
        this.portafolioService = portafolioService;
    }

    @GetMapping("/portfolio")
    public PortafolioResponse getPortafolio(@PathVariable String userId) {
        return portafolioService.getPortafolio(userId);
    }

    @PostMapping("/deposits")
    public DepositoResponse deposito(@PathVariable String userId, @Valid @RequestBody DepositoRequest request) {
        return portafolioService.deposito(userId, request);
    }

    @PostMapping("/portfolio/trades")
    public PortafolioResponse applyTrade(@PathVariable String userId, @Valid @RequestBody TradeAdjustmentRequest request) {
        return portafolioService.aplicarTrade(userId, request);
    }
}

