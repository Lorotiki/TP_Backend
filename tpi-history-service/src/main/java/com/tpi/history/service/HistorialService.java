package com.tpi.history.service;

import com.tpi.history.dto.HistorialEventoRequest;
import com.tpi.history.dto.HistorialEventoResponse;
import com.tpi.history.entity.HistorialEvento;
import com.tpi.history.entity.OperacionUsuarioView;
import com.tpi.history.repository.HistorialEventoRepository;
import com.tpi.history.repository.OperacionUsuarioViewRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Service
public class HistorialService {

    private final HistorialEventoRepository historialEventoRepository;
    private final OperacionUsuarioViewRepository operacionUsuarioViewRepository;

    public HistorialService(HistorialEventoRepository historialEventoRepository,
                            OperacionUsuarioViewRepository operacionUsuarioViewRepository) {
        this.historialEventoRepository = historialEventoRepository;
        this.operacionUsuarioViewRepository = operacionUsuarioViewRepository;
    }

    @Transactional
    public HistorialEventoResponse registraEvento(HistorialEventoRequest request) {

        HistorialEvento evento = new HistorialEvento();

        evento.setEventId(request.eventId() != null ? request.eventId() : UUID.randomUUID());
        evento.setEventType(request.eventType());
        evento.setUserId(request.userId());
        evento.setOrderId(request.orderId());
        evento.setCorrelationId(request.correlationId()); 
        evento.setCausationId(request.causationId()); 
        evento.setPayloadJson(request.payloadJson());

        HistorialEvento historialEventoGrabado = historialEventoRepository.save(evento);

        persistModeloLectura(historialEventoGrabado);

        return toResponse(historialEventoGrabado);
    }

    public List<HistorialEventoResponse> getHistorialUsuario(String userId) {
        return historialEventoRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<HistorialEventoResponse> getHistorialCompleto() {
        return historialEventoRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void persistModeloLectura(HistorialEvento historialEventoGuardado) {

        if (historialEventoGuardado.getUserId() == null || historialEventoGuardado.getUserId().isBlank()) {
            return;
        }

        OperacionUsuarioView item = new OperacionUsuarioView();
        item.setUserId(historialEventoGuardado.getUserId());
        item.setOperationType(historialEventoGuardado.getEventType());
        item.setSymbol((String) historialEventoGuardado.getPayloadJson().get("symbol"));
        item.setAmountArs(extraerMonto(historialEventoGuardado.getPayloadJson().get("amountArs")));

        operacionUsuarioViewRepository.save(item);
    }

    private BigDecimal extraerMonto(Object valor) {
        if (valor == null) {
            return null;
        }
        
        if (valor instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue())
                    .setScale(2, java.math.RoundingMode.HALF_UP); // Redondeo escolar/financiero estándar
        }

        return new BigDecimal(valor.toString())
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private HistorialEventoResponse toResponse(HistorialEvento event) {
        return new HistorialEventoResponse(
                event.getEventId(),
                event.getEventType(),
                event.getUserId(),
                event.getOrderId(),
                event.getCorrelationId(),
                event.getCausationId(),
                event.getPayloadJson(),
                event.getOccurredAt()
        );
    }
}