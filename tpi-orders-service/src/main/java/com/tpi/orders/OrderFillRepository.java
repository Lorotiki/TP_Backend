package com.tpi.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderFillRepository extends JpaRepository<OrderFill, UUID> {
}

