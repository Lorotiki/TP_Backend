package com.tpi.orders.repository;

import com.tpi.orders.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Query("""
            select o from OrderEntity o
            where o.symbol = :symbol
              and o.side = :side
              and o.status in ('PENDING', 'PARTIALLY_FILLED')
            order by o.limitPrice asc, o.createdAt asc
            """)
    List<OrderEntity> findMatchingSellers(String symbol, String side);

    @Query("""
            select o from OrderEntity o
            where o.userId = :userId
            order by o.createdAt desc
            """)
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("""
            select o from OrderEntity o
            where o.userId = :userId
              and o.symbol = :symbol
              and o.side = 'SELL'
              and o.status in ('PENDING', 'PARTIALLY_FILLED')
            """)
    List<OrderEntity> findOpenSellOrders(String userId, String symbol);
}

