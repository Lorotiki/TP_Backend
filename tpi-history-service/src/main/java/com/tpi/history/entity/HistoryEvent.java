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
@Table(name = "history_events", schema = "history")
public class HistoryEvent {

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

    // to-do cambiar mapeo de Entidad a Bda relacion 
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false)
    private Map<String, Object> payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // TO-DO BUSCAR ALTERNATIVA CON LOMBOK
    @PrePersist
    void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    // to-do ELIMINAR GETTERS Y SETTERS, USAR LOMBOK @DATA
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public UUID getCausationId() {
        return causationId;
    }

    public void setCausationId(UUID causationId) {
        this.causationId = causationId;
    }

    public Map<String, Object> getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(Map<String, Object> payloadJson) {
        this.payloadJson = payloadJson;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}

