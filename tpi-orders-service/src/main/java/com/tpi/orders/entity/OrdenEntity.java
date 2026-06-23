package com.tpi.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "orders")
public class OrdenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "symbol", nullable = false)
    private String simbolo;

    @Column(name = "side", nullable = false)
    private String lado;

    @Column(name = "quantity", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "remaining_quantity", nullable = false)
    private BigDecimal cantidadRestante;

    @Column(name = "limit_price", nullable = false)
    private BigDecimal precioLimite;

    @Column(name = "status", nullable = false)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        var now = LocalDateTime.now();
        creadoEn = now;
        actualizadoEn = now;
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}

