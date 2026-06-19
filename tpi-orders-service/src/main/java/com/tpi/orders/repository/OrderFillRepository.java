package com.tpi.orders.repository;

import com.tpi.orders.entity.OrderFill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderFillRepository extends JpaRepository<OrderFill, UUID> {
}

