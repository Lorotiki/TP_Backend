package com.tpi.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByAccountId(UUID accountId);
    Optional<Position> findByAccountIdAndSymbol(UUID accountId, String symbol);
}

