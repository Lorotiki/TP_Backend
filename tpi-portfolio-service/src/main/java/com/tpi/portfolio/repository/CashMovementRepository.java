package com.tpi.portfolio.repository;

import com.tpi.portfolio.entity.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {
}

