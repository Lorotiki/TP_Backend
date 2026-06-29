package com.tpi.history.controller;

import com.tpi.history.dto.HistorialEventoRequest;
import com.tpi.history.dto.HistorialEventoResponse;

import com.tpi.history.service.HistorialService;
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
public class HistorialController {

    private final HistorialService historialService;

    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    @PostMapping("/events")
    public HistorialEventoResponse registraEvento(@Valid @RequestBody HistorialEventoRequest request) {
        return historialService.registraEvento(request);
    }

    @GetMapping("/users/{userId}/history")
    public List<HistorialEventoResponse> getHistorialUsuario(@PathVariable String userId) {
        return historialService.getHistorialUsuario(userId);
    }

    @GetMapping("/admin/history")
    public List<HistorialEventoResponse> getHistorialCompleto() {
        return historialService.getHistorialCompleto();
    }
}

