package com.tpi.portfolio.repository;

import com.tpi.portfolio.entity.Posicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosicionRepository extends JpaRepository<Posicion, UUID> {
    List<Posicion> findByAccountId(UUID accountId);
    Optional<Posicion> findByAccountIdAndSymbol(UUID accountId, String symbol);
}

