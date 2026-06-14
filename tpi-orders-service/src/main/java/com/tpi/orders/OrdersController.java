package com.tpi.orders;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping("/orders/buy")
    public OrderResponse buy(@Valid @RequestBody OrderRequest request) {
        return ordersService.buy(request);
    }

    @PostMapping("/orders/sell")
    public OrderResponse sell(@Valid @RequestBody OrderRequest request) {
        return ordersService.sell(request);
    }

    @GetMapping("/users/{userId}/orders")
    public List<OrderResponse> getOrders(@PathVariable String userId) {
        return ordersService.getOrdersByUser(userId);
    }
}

