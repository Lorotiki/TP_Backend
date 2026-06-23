package com.tpi.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteLocatorConfig {

    @Value("${services.uri.market-data:http://localhost:8081}")
    private String marketDataUri;

    @Value("${services.uri.portfolio:http://localhost:8082}")
    private String portfolioUri;

    @Value("${services.uri.orders:http://localhost:8083}")
    private String ordersUri;

    @Value("${services.uri.history:http://localhost:8084}")
    private String historyUri;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("market-data-service", r -> r.path("/quotes/**")
                        .uri(marketDataUri))
                .route("portfolio-service", r -> r.path("/users/{userId}/portfolio/**", "/users/{userId}/deposits/**")
                        .uri(portfolioUri))
                .route("orders-service", r -> r.path("/orders/**", "/users/{userId}/orders/**")
                        .uri(ordersUri))
                .route("history-service", r -> r.path("/users/{userId}/history/**", "/admin/history/**", "/events/**")
                        .uri(historyUri))
                .build();
    }
}
