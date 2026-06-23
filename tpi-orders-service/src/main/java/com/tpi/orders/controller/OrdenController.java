package com.tpi.orders.controller;

import com.tpi.orders.dto.OrdenRequest;
import com.tpi.orders.dto.OrdenResponse;
import com.tpi.orders.service.OrdenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @PostMapping
    public OrdenResponse createOrden(@Valid @RequestBody OrdenRequest request) {
        return ordenService.createOrden(request);
    }

    @GetMapping("/user/{userId}")
    public List<OrdenResponse> getOrdenes(@PathVariable String userId) {
        return ordenService.getOrdenesByUsuario(userId);
    }
}
