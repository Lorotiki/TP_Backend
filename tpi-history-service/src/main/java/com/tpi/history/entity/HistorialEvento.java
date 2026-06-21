package com.tpi.history.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "history_events")
public class HistorialEvento {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "causation_id")
    private UUID causationId;

/*
@JdbcTypeCode(SqlTypes.JSON), le estás diciendo a Hibernate:
"Che, cuando guardes este Map, convertilo automáticamente a una cadena de texto JSON (un String formateado) y metelo en la columna payload_json de la base de datos.
Y cuando lo leas, volvé a transformarlo en un objeto Map".
 */

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false)
    private Map<String, Object> payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /*
    Sirve para marcar un método que se tiene que ejecutar automáticamente justo antes de que el objeto se inserte por primera vez en la base de datos
    (cuando se hace un save o persist).
     */
    @PrePersist
    void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}

