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
        var order = createOrder(request, "SELL");
        validateSellAvailability(order);
        order.setStatus("PENDING");
        orderRepository.save(order);
        recordHistory("SELL_ORDER_CREATED", order, order.getQuantity(), order.getRemainingQuantity(), order.getLimitPrice(), "Orden de venta registrada");
        return toResponse(order, BigDecimal.ZERO, order.getRemainingQuantity(), order.getLimitPrice(), "Orden de venta registrada");
    }

    @Transactional
    public OrderResponse buy(OrderRequest request) {
        var marketQuote = fetchQuote(request.symbol());
        var order = createOrder(request, "BUY");
        validateBuyBalance(order, marketQuote.price());
        orderRepository.save(order);

        var availableSellers = orderRepository.findMatchingSellers(order.getSymbol(), "SELL");
        var matchedQuantity = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal executionPrice = marketQuote.price().setScale(4, RoundingMode.HALF_UP);

        for (var seller : availableSellers) {
            if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
            if (seller.getUserId().equals(order.getUserId())) {
                continue;
            }
            if (seller.getLimitPrice().compareTo(order.getLimitPrice()) > 0) {
                continue;
            }

            var fillQty = seller.getRemainingQuantity().min(order.getRemainingQuantity()).setScale(4, RoundingMode.HALF_UP);
            if (fillQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            executionPrice = seller.getLimitPrice().setScale(4, RoundingMode.HALF_UP);
            applyTradeToPortfolio(order.getUserId(), "BUY", order.getSymbol(), fillQty, executionPrice, order.getId().toString());
            applyTradeToPortfolio(seller.getUserId(), "SELL", seller.getSymbol(), fillQty, executionPrice, seller.getId().toString());

            seller.setRemainingQuantity(seller.getRemainingQuantity().subtract(fillQty));
            seller.setStatus(resolveStatus(seller.getRemainingQuantity(), seller.getQuantity()));
            order.setRemainingQuantity(order.getRemainingQuantity().subtract(fillQty));
            order.setStatus(resolveStatus(order.getRemainingQuantity(), order.getQuantity()));

            var fill = new OrderFill();
            fill.setBuyOrderId(order.getId());
            fill.setSellOrderId(seller.getId());
            fill.setSymbol(order.getSymbol());
            fill.setQuantity(fillQty);
            fill.setPriceArs(executionPrice);
            orderFillRepository.save(fill);
            orderRepository.save(seller);

            matchedQuantity = matchedQuantity.add(fillQty);
        }

        if (matchedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus("REJECTED");
            order.setRemainingQuantity(order.getQuantity());
            orderRepository.save(order);
            recordHistory("BUY_ORDER_REJECTED", order, BigDecimal.ZERO, order.getRemainingQuantity(), marketQuote.price(), "No existe oferta compatible");
            return toResponse(order, BigDecimal.ZERO, order.getRemainingQuantity(), marketQuote.price(), "No existe oferta compatible");
        }

        orderRepository.save(order);
        recordHistory("BUY_ORDER_EXECUTED", order, matchedQuantity, order.getRemainingQuantity(), executionPrice,
                order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0 ? "Orden ejecutada" : "Orden parcialmente ejecutada");
        return toResponse(order, matchedQuantity, order.getRemainingQuantity(), executionPrice,
                order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0 ? "Orden ejecutada" : "Orden parcialmente ejecutada");
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

    private void validateBuyBalance(OrderEntity order, BigDecimal marketPrice) {
        var portfolio = fetchPortfolio(order.getUserId());
        var estimatedTotal = marketPrice.setScale(4, RoundingMode.HALF_UP)
                .multiply(order.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
        if (portfolio.balanceArs().compareTo(estimatedTotal) < 0) {
            order.setStatus("REJECTED");
            order.setRemainingQuantity(order.getQuantity());
            orderRepository.save(order);
            recordHistory("BUY_ORDER_REJECTED", order, BigDecimal.ZERO, order.getRemainingQuantity(), marketPrice,
                    "Saldo insuficiente para realizar la compra");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente para realizar la compra");
        }
    }

    private void validateSellAvailability(OrderEntity order) {
        var portfolio = fetchPortfolio(order.getUserId());
        var ownedQuantity = portfolio.positions() == null ? BigDecimal.ZERO : portfolio.positions().stream()
                .filter(position -> order.getSymbol().equals(position.symbol()))
                .map(PortfolioClientPositionResponse::quantity)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        var reservedQuantity = orderRepository.findOpenSellOrders(order.getUserId(), order.getSymbol()).stream()
                .map(OrderEntity::getRemainingQuantity)
                .reduce(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), BigDecimal::add);
        var availableToSell = ownedQuantity.subtract(reservedQuantity);

        if (availableToSell.compareTo(order.getQuantity()) < 0) {
            order.setStatus("REJECTED");
            order.setRemainingQuantity(order.getQuantity());
            orderRepository.save(order);
            recordHistory("SELL_ORDER_REJECTED", order, BigDecimal.ZERO, order.getRemainingQuantity(), order.getLimitPrice(),
                    "Tenencia insuficiente para registrar la orden de venta");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenencia insuficiente para registrar la orden de venta");
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
}

