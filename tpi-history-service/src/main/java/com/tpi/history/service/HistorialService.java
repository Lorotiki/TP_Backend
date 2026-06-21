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

/**
 * Servicio encargado de la lógica de negocio para el historial de eventos.
 * Conecta los controladores con el acceso a datos.
 */
@Service // Indica a Spring que esta clase es un componente de servicio (Bean) para inyección de dependencias.
public class HistorialService {

    // Dependencias finales (inmutables) para interactuar con la base de datos
    private final HistorialEventoRepository historialEventoRepository;
    private final OperacionUsuarioViewRepository operacionUsuarioViewRepository;

    /**
     * Constructor para la inyección de dependencias.
     * Al ser un único constructor, Spring inyecta automáticamente los repositorios.
     */
    public HistorialService(HistorialEventoRepository historialEventoRepository,
                            OperacionUsuarioViewRepository operacionUsuarioViewRepository) {
        this.historialEventoRepository = historialEventoRepository;
        this.operacionUsuarioViewRepository = operacionUsuarioViewRepository;
    }

    /**
     * Registra un nuevo evento en el sistema.
     * @Transactional asegura que toda la operación sea atómica: si falla la escritura del
     * evento o del Read Model, se hace un rollback completo para no dejar datos corruptos.
     */
    @Transactional
    public HistorialEventoResponse registraEvento(HistorialEventoRequest request) {
        // Se crea la entidad JPA que mapeará a la tabla de la base de datos
        var evento = new HistorialEvento();

        // Asignación de ID: Si la petición no trae un UUID, se genera uno aleatorio en el momento
        // Nota: Esto complementa tu lógica de seguridad @PrePersist que vimos antes
        evento.setEventId(request.eventId() != null ? request.eventId() : UUID.randomUUID());

        // Mapeo directo de los campos desde el Record (DTO) hacia la Entidad
        evento.setEventType(request.eventType());
        evento.setUserId(request.userId());
        evento.setOrderId(request.orderId());

        // Datos cruciales para la trazabilidad y auditoría del microservicio
        evento.setCorrelationId(request.correlationId()); // Une todo el flujo punta a punta
        evento.setCausationId(request.causationId());     // Identifica qué disparó este evento

        // Asigna el mapa dinámico que Hibernate guardará como formato JSON en la BD
        evento.setPayloadJson(request.payloadJson());

        // Persistencia: Guarda el evento en la tabla principal de eventos
        var historialEventoGrabado = historialEventoRepository.save(evento);

        // CQRS Pattern / Read Model: Actualiza de forma asincrónica o paralela una vista optimizada de lectura
        persistModeloLectura(historialEventoGrabado);

        // Retorna el DTO de respuesta transformado para el cliente/frontend
        return toResponse(historialEventoGrabado);
    }

    /**
     * Recupera el historial específico de un usuario, ordenado cronológicamente desde el más reciente.
     */
    public List<HistorialEventoResponse> getHistorialUsuario(String userId) {
        // Uso de Streams de Java para transformar cada entidad de la lista en un DTO de respuesta
        return historialEventoRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
                .map(this::toResponse) // Equivalente a: event -> toResponse(event)
                .toList();
    }

    /**
     * Recupera todo el historial global del sistema ordenado por fecha de creación descendente.
     */
    public List<HistorialEventoResponse> getHistorialCompleto() {
        return historialEventoRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Método auxiliar privado para persistir el "Read Model" (Modelo de Lectura).
     * Extrae información estructurada a partir del JSON dinámico guardado en el evento.
     */
    private void persistModeloLectura(HistorialEvento historialEventoGuardado) {
        // Validación defensiva: Si no hay un usuario válido asociado, no se registra la vista operativa
        if (historialEventoGuardado.getUserId() == null || historialEventoGuardado.getUserId().isBlank()) {
            return;
        }

        // Instancia la entidad de la vista de operaciones del usuario
        var item = new OperacionUsuarioView();
        item.setUserId(historialEventoGuardado.getUserId());
        item.setOperationType(historialEventoGuardado.getEventType());

        // Extracción dinámica: Al ser payloadJson un Map<String, Object>, se castea el valor a String
        item.setSymbol((String) historialEventoGuardado.getPayloadJson().get("symbol"));

        // Extracción segura del monto: Convierte el Object dynamic a un formato monetario estricto (BigDecimal)
        item.setAmountArs(extraerMonto(historialEventoGuardado.getPayloadJson().get("amountArs")));

        // Guarda en la tabla optimizada para consultas de operaciones
        operacionUsuarioViewRepository.save(item);
    }

    /**
     * Convierte de manera segura cualquier representación numérica dinámica (como la que viene
     * deserializada de un JSON de Jackson) a un tipo adecuado para finanzas (BigDecimal con 2 decimales).
     */
    private BigDecimal extraerMonto(Object valor) {
        if (valor == null) {
            return null;
        }

        // Si Jackson lo deserializó como una subclase de Number (Integer, Double, Long, etc.)
        if (valor instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue())
                    .setScale(2, java.math.RoundingMode.HALF_UP); // Redondeo escolar/financiero estándar
        }

        // Si viene como String u otra representación, intenta parsearlo directamente de su texto
        return new BigDecimal(valor.toString())
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Mapper manual: Transforma la entidad JPA 'HistoryEvent' al Record de salida 'HistoryEventResponse'.
     * Mantiene las capas de la arquitectura desacopladas (el exterior nunca ve la entidad de base de datos cruda).
     */
    private HistorialEventoResponse toResponse(HistorialEvento event) {
        return new HistorialEventoResponse(
                event.getEventId(),
                event.getEventType(),
                event.getUserId(),
                event.getOrderId(),
                event.getCorrelationId(),
                event.getCausationId(),
                event.getPayloadJson(),
                event.getOccurredAt() // Este campo fue generado automáticamente en la entidad gracias al @PrePersist!
        );
    }
}