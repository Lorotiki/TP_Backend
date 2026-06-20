package com.tpi.orders.service;

import com.tpi.orders.dto.HistoryEventClientRequest;
import com.tpi.orders.dto.OrderRequest;
import com.tpi.orders.dto.OrderResponse;
import com.tpi.orders.dto.PortfolioClientPositionResponse;
import com.tpi.orders.dto.PortfolioClientResponse;
import com.tpi.orders.dto.PortfolioTradeClientRequest;
import com.tpi.orders.dto.QuoteClientResponse;
import com.tpi.orders.entity.OrderEntity;
import com.tpi.orders.entity.OrderFill;
import com.tpi.orders.repository.OrderFillRepository;
import com.tpi.orders.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class OrdersService {

    private final OrderRepository orderRepository;
    private final OrderFillRepository orderFillRepository;
    private final RestTemplate restTemplate;
    private final String marketDataBaseUrl;
    private final String portfolioBaseUrl;
    private final String historyBaseUrl;

    public OrdersService(OrderRepository orderRepository,
                         OrderFillRepository orderFillRepository,
                         RestTemplate restTemplate,
                         @Value("${clients.market-data.base-url}") String marketDataBaseUrl,
                         @Value("${clients.portfolio.base-url}") String portfolioBaseUrl,
                         @Value("${clients.history.base-url}") String historyBaseUrl) {
        this.orderRepository = orderRepository;
        this.orderFillRepository = orderFillRepository;
        this.restTemplate = restTemplate;
        this.marketDataBaseUrl = marketDataBaseUrl;
        this.portfolioBaseUrl = portfolioBaseUrl;
        this.historyBaseUrl = historyBaseUrl;
    }

    @Transactional
    public OrderResponse sell(OrderRequest request) {
        log.info("▶️ Procesando orden de venta: userId={}, symbol={}, qty={}, priceLimit={}",
                request.userId(), request.symbol(), request.quantity(), request.priceLimit());
        try {
            var order = createOrder(request, "SELL");
            validateSellAvailability(order);
            order.setStatus("PENDING");
            orderRepository.save(order);

            log.info("✅ Orden de venta registrada: orderId={}, status={}",
                    order.getId(), order.getStatus());

            recordHistory("SELL_ORDER_CREATED", order, order.getQuantity(),
                    order.getRemainingQuantity(), order.getLimitPrice(),
                    "Orden de venta registrada");

            return toResponse(order, BigDecimal.ZERO, order.getRemainingQuantity(),
                    order.getLimitPrice(), "Orden de venta registrada");
        } catch (Exception e) {
            log.error("❌ Error procesando orden de venta: userId={}, symbol={}",
                    request.userId(), request.symbol(), e);
            throw e;
        }
    }

    @Transactional
    public OrderResponse buy(OrderRequest request) {
        log.info("▶️ Procesando orden de compra: userId={}, symbol={}, qty={}, priceLimit={}",
                request.userId(), request.symbol(), request.quantity(), request.priceLimit());

        try {
            // Consultar cotización
            var marketQuote = fetchQuote(request.symbol());
            log.debug("✓ Cotización obtenida: symbol={}, price={}, currency={}",
                    marketQuote.symbol(), marketQuote.price(), marketQuote.currency());

            var order = createOrder(request, "BUY");
            log.debug("✓ Orden creada: orderId={}", order.getId());

            // Validar balance
            validateBuyBalance(order, marketQuote.price());
            log.debug("✓ Balance validado: requiredBalance={} ARS",
                    order.getQuantity().multiply(order.getLimitPrice()));

            orderRepository.save(order);
            log.debug("✓ Orden persistida en BD");

            // Matching
            var availableSellers = orderRepository.findMatchingSellers(order.getSymbol(), "SELL");
            log.debug("✓ Encontradas {} órdenes de venta disponibles", availableSellers.size());

            var matchedQuantity = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            for (var seller : availableSellers) {
                if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    log.debug("✓ Cantidad completa satisfecha, saliendo del loop");
                    break;
                }

                if (seller.getUserId().equals(order.getUserId())) {
                    log.debug("⊘ Saltando venta del mismo usuario: userId={}", order.getUserId());
                    continue;
                }

                if (seller.getLimitPrice().compareTo(order.getLimitPrice()) > 0) {
                    log.debug("⊘ Precio no coincide: seller.price={} > buyer.price={}",
                            seller.getLimitPrice(), order.getLimitPrice());
                    continue;
                }

                var fillQty = seller.getRemainingQuantity().min(order.getRemainingQuantity())
                        .setScale(4, RoundingMode.HALF_UP);

                if (fillQty.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("⊘ Cantidad inválida: fillQty={}", fillQty);
                    continue;
                }

                log.info("✓ MATCH encontrado: buyer={}, seller={}, qty={}, price={}",
                        order.getUserId(), seller.getUserId(), fillQty, seller.getLimitPrice());

                var executionPrice = seller.getLimitPrice().setScale(4, RoundingMode.HALF_UP);

                applyTradeToPortfolio(order.getUserId(), "BUY", order.getSymbol(), fillQty,
                        executionPrice, order.getId().toString());
                applyTradeToPortfolio(seller.getUserId(), "SELL", seller.getSymbol(), fillQty,
                        executionPrice, seller.getId().toString());

                seller.setRemainingQuantity(seller.getRemainingQuantity().subtract(fillQty));
                seller.setStatus(resolveStatus(seller.getRemainingQuantity(), seller.getQuantity()));
                order.setRemainingQuantity(order.getRemainingQuantity().subtract(fillQty));
                order.setStatus(resolveStatus(order.getRemainingQuantity(), order.getQuantity()));

                matchedQuantity = matchedQuantity.add(fillQty);
            }

            orderRepository.saveAll(List.of(order));
            recordHistory("BUY_ORDER_EXECUTED", order, matchedQuantity, order.getRemainingQuantity(),
                    order.getLimitPrice(), "Orden de compra ejecutada");

            log.info("✅ Orden completada: orderId={}, matchedQty={}, remainingQty={}, status={}",
                    order.getId(), matchedQuantity, order.getRemainingQuantity(), order.getStatus());

            return toResponse(order, matchedQuantity, order.getRemainingQuantity(),
                    order.getLimitPrice(), "Orden de compra ejecutada");
        } catch (Exception e) {
            log.error("❌ Error procesando orden de compra: userId={}, symbol={}",
                    request.userId(), request.symbol(), e);
            throw e;
        }
    }

    public List<OrderResponse> getOrdersByUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> toResponse(order,
                        order.getQuantity().subtract(order.getRemainingQuantity()),
                        order.getRemainingQuantity(),
                        order.getLimitPrice(),
                        "Consulta de órdenes"))
                .toList();
    }

    private void applyTradeToPortfolio(String userId, String side, String symbol, BigDecimal quantity, BigDecimal priceArs, String referenceId) {
        var request = new PortfolioTradeClientRequest(side, symbol, quantity, priceArs, referenceId);
        try {
            restTemplate.postForEntity(portfolioBaseUrl + "/users/" + userId + "/portfolio/trades", request, Void.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo actualizar portfolio-service", e);
        }
    }

    /**
     * Valida que el usuario tenga saldo suficiente para la compra
     */
    private void validateBuyBalance(OrderEntity order, BigDecimal marketPrice) {
        log.debug("Validando balance para compra: userId={}, qty={}, limitPrice={}",
                order.getUserId(), order.getQuantity(), order.getLimitPrice());

        // Consultar portafolio del usuario
        try {
            PortfolioClientResponse portfolio = restTemplate.getForObject(
                    portfolioBaseUrl + "/users/" + order.getUserId() + "/portfolio",
                    PortfolioClientResponse.class
            );

            if (portfolio == null) {
                log.warn("⚠️ Portfolio no encontrado para userId: {}", order.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portfolio no encontrado para el usuario"
                );
            }

            // Calcular monto requerido
            BigDecimal requiredAmount = order.getQuantity()
                    .multiply(order.getLimitPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            log.debug("Balance requerido: {} ARS, Balance disponible: {} ARS",
                    requiredAmount, portfolio.balanceArs());

            // Validar saldo
            if (portfolio.balanceArs().compareTo(requiredAmount) < 0) {
                log.warn("❌ Saldo insuficiente: usuario={}, requerido={}, disponible={}",
                        order.getUserId(), requiredAmount, portfolio.balanceArs());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Saldo insuficiente. Requerido: %s ARS, Disponible: %s ARS",
                                requiredAmount, portfolio.balanceArs()
                        )
                );
            }

            log.debug("✓ Balance validado correctamente");

        } catch (Exception e) {
            log.error("Error validando balance: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al validar saldo del usuario"
            );
        }
    }

    private PortfolioClientResponse fetchPortfolio(String userId) {
        try {
            return restTemplate.getForObject(portfolioBaseUrl + "/users/" + userId + "/portfolio", PortfolioClientResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar portfolio-service", e);
        }
    }

    private QuoteClientResponse fetchQuote(String symbol) {
        try {
            return restTemplate.getForObject(marketDataBaseUrl + "/quotes/" + symbol, QuoteClientResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar market-data-service", e);
        }
    }

    private void recordHistory(String eventType,
                               OrderEntity order,
                               BigDecimal matchedQuantity,
                               BigDecimal remainingQuantity,
                               BigDecimal priceArs,
                               String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", order.getSymbol());
        payload.put("side", order.getSide());
        payload.put("quantity", order.getQuantity());
        payload.put("matchedQuantity", matchedQuantity);
        payload.put("remainingQuantity", remainingQuantity);
        payload.put("priceArs", priceArs);
        payload.put("amountArs", priceArs.multiply(matchedQuantity).setScale(2, RoundingMode.HALF_UP));
        payload.put("message", message);

        var request = new HistoryEventClientRequest(UUID.randomUUID(), eventType, order.getUserId(), order.getId(), UUID.randomUUID(), null, payload);
        try {
            restTemplate.postForEntity(historyBaseUrl + "/events", request, Void.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo registrar el historial", e);
        }
    }

    private OrderEntity createOrder(OrderRequest request, String side) {
        var order = new OrderEntity();
        order.setUserId(request.userId());
        order.setSymbol(request.symbol().trim().toUpperCase(Locale.ROOT));
        order.setSide(side);
        order.setQuantity(request.quantity().setScale(4, RoundingMode.HALF_UP));
        order.setRemainingQuantity(request.quantity().setScale(4, RoundingMode.HALF_UP));
        order.setLimitPrice(request.priceLimit().setScale(4, RoundingMode.HALF_UP));
        order.setStatus("PENDING");
        return order;
    }

    private String resolveStatus(BigDecimal remainingQuantity, BigDecimal totalQuantity) {
        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return "FILLED";
        }
        if (remainingQuantity.compareTo(totalQuantity) < 0) {
            return "PARTIALLY_FILLED";
        }
        return "PENDING";
    }

    private OrderResponse toResponse(OrderEntity order,
                                     BigDecimal matchedQuantity,
                                     BigDecimal remainingQuantity,
                                     BigDecimal executionPriceArs,
                                     String message) {
        return new OrderResponse(order.getId(), order.getStatus(), matchedQuantity, remainingQuantity, executionPriceArs, message);
    }

    /**
     * Valida que el usuario tenga las acciones para vender
     */
    private void validateSellAvailability(OrderEntity order) {
        log.debug("Validando disponibilidad de acciones para venta: userId={}, symbol={}, qty={}",
                order.getUserId(), order.getSymbol(), order.getQuantity());

        try {
            PortfolioClientResponse portfolio = restTemplate.getForObject(
                    portfolioBaseUrl + "/users/" + order.getUserId() + "/portfolio",
                    PortfolioClientResponse.class
            );

            if (portfolio == null) {
                log.warn("⚠️ Portfolio no encontrado para userId: {}", order.getUserId());
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portfolio no encontrado para el usuario"
                );
            }

            // Buscar la posición del símbolo
            var position = portfolio.positions().stream()
                    .filter(p -> p.symbol().equalsIgnoreCase(order.getSymbol()))
                    .findFirst()
                    .orElse(null);

            if (position == null || position.quantity().compareTo(BigDecimal.ZERO) == 0) {
                log.warn("❌ Posición no disponible para venta: usuario={}, symbol={}",
                        order.getUserId(), order.getSymbol());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No tiene acciones de " + order.getSymbol() + " para vender"
                );
            }

            if (position.quantity().compareTo(order.getQuantity()) < 0) {
                log.warn("❌ Cantidad insuficiente de acciones: usuario={}, symbol={}, " +
                                "requerida={}, disponible={}",
                        order.getUserId(), order.getSymbol(),
                        order.getQuantity(), position.quantity());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Cantidad insuficiente de %s. Tiene: %s, quiere vender: %s",
                                order.getSymbol(), position.quantity(), order.getQuantity()
                        )
                );
            }

            log.debug("✓ Disponibilidad validada: posee {} de {}",
                    position.quantity(), order.getSymbol());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validando disponibilidad de acciones: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al validar disponibilidad de acciones"
            );
        }
    }
}

