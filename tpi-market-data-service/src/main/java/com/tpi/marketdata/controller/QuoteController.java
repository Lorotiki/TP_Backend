package com.tpi.marketdata.controller;

import com.tpi.marketdata.dto.QuoteResponse;
import com.tpi.marketdata.service.MarketQuoteService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final MarketQuoteService marketQuoteService;

    public QuoteController(MarketQuoteService marketQuoteService) {
        this.marketQuoteService = marketQuoteService;
    }

    @GetMapping("/{symbol}")
    public QuoteResponse getQuote(@PathVariable String symbol) {
        return marketQuoteService.getQuote(symbol);
    }
}

