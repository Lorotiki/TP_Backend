package com.tpi.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "order_fills")
public class OrdenFill {

    @Id
    private UUID id;

    @Column(name = "buy_order_id", nullable = false)
    private UUID compraOrdenId;

    @Column(name = "sell_order_id", nullable = false)
    private UUID ventaOrdenId;

    @Column(name = "symbol", nullable = false)
    private String simbolo;

    @Column(name = "quantity", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "price_ars", nullable = false)
    private BigDecimal precioArs;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime ejecutadoEn;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        ejecutadoEn = LocalDateTime.now();
    }
}


