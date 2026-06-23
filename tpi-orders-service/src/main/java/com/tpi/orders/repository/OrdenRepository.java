package com.tpi.orders.repository;

import com.tpi.orders.entity.OrdenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrdenRepository extends JpaRepository<OrdenEntity, UUID> {

    @Query("""
            select o from OrdenEntity o
            where o.simbolo = :symbol
              and o.lado = :side
              and o.estado in ('PENDING', 'PARTIALLY_FILLED')
            order by o.precioLimite asc, o.creadoEn asc
            """)
    List<OrdenEntity> findCoincidenciasVentas(@Param("symbol") String symbol, @Param("side") String side);

    @Query("""
            select o from OrdenEntity o
            where o.userId = :userId
            order by o.creadoEn desc
            """)
    List<OrdenEntity> findByUsuarioIdOrdenByCreatedAtDesc(@Param("userId") String usuarioId);

    @Query("""
            select o from OrdenEntity o
            where o.userId = :userId
              and o.simbolo = :symbol
              and o.lado = 'SELL'
              and o.estado in ('PENDING', 'PARTIALLY_FILLED')
            """)
    List<OrdenEntity> findOrdenesVentaAbiertas(@Param("userId") String usuarioId, @Param("symbol") String simbolo);
}

