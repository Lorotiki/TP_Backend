package com.tpi.portfolio.repository;

import com.tpi.portfolio.entity.MovimientoDinero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MovimientoDineroRepository extends JpaRepository<MovimientoDinero, UUID> {
}

