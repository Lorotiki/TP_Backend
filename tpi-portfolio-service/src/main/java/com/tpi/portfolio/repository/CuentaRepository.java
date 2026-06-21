package com.tpi.portfolio.repository;

import com.tpi.portfolio.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {
    Optional<Cuenta> findByUserId(String userId);
}

