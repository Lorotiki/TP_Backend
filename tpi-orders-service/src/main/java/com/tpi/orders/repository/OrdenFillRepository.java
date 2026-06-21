package com.tpi.orders.repository;

import com.tpi.orders.entity.OrdenFill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrdenFillRepository extends JpaRepository<OrdenFill, UUID> {
}

