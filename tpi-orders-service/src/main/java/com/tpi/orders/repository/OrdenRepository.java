package com.tpi.orders.repository;

import com.tpi.orders.entity.OrdenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrdenRepository extends JpaRepository<OrdenEntity, UUID> {

    @Query("""
            select o from OrderEntity o
            where o.symbol = :symbol
              and o.side = :side
              and o.status in ('PENDING', 'PARTIALLY_FILLED')
            order by o.limitPrice asc, o.createdAt asc
            """)
    List<OrdenEntity> findCoincidenciasVentas(String symbol, String side);

    @Query("""
            select o from OrderEntity o
            where o.userId = :userId
            order by o.createdAt desc
            """)
    List<OrdenEntity> findByUsuarioIdOrdenByCreatedAtDesc(String usuarioId);

    @Query("""
            select o from OrderEntity o
            where o.userId = :userId
              and o.symbol = :symbol
              and o.side = 'SELL'
              and o.status in ('PENDING', 'PARTIALLY_FILLED')
            """)
    List<OrdenEntity> findOrdenesVentaAbiertas(String usuarioId, String simbolo);
}

