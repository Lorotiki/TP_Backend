package com.tpi.portfolio.controller;

import com.tpi.portfolio.dto.DepositRequest;
import com.tpi.portfolio.dto.DepositResponse;
import com.tpi.portfolio.dto.PortfolioResponse;
import com.tpi.portfolio.dto.TradeAdjustmentRequest;
import com.tpi.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio(@PathVariable String userId) {
        return portfolioService.getPortfolio(userId);
    }

    @PostMapping("/deposits")
    public DepositResponse deposit(@PathVariable String userId, @Valid @RequestBody DepositRequest request) {
        return portfolioService.deposit(userId, request);
    }

    @PostMapping("/portfolio/trades")
    public PortfolioResponse applyTrade(@PathVariable String userId, @Valid @RequestBody TradeAdjustmentRequest request) {
        return portfolioService.applyTrade(userId, request);
    }
}

