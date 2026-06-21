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
/*
@RequestBody: Le dice a Spring que tome el cuerpo del JSON que envía el cliente y lo transforme automáticamente en el objeto HistoryEventRequest.

@Valid: Activa las validaciones del Record (como el @NotBlank y @NotNull). Si la petición viene incompleta o con datos inválidos,
Spring frena el proceso acá y devuelve un error antes de tocar la base de datos.
 */
    @PostMapping("/events")
    public HistorialEventoResponse registraEvento(@Valid @RequestBody HistorialEventoRequest request) {
        return historialService.registraEvento(request);
    }
/*
@PathVariable String userId: Le indica a Spring que la variable {userId} que viaja incrustada directamente en la URL dinámica
se debe extraer y asignar al parámetro userId del método.
 */
    @GetMapping("/users/{userId}/history")
    public List<HistorialEventoResponse> getHistorialUsuario(@PathVariable String userId) {
        return historialService.getHistorialUsuario(userId);
    }

    @GetMapping("/admin/history")
    public List<HistorialEventoResponse> getHistorialCompleto() {
        return historialService.getHistorialCompleto();
    }
}

