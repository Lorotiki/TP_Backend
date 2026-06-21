package com.tpi.history.repository;

import com.tpi.history.entity.HistorialEvento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialEventoRepository extends JpaRepository<HistorialEvento, UUID> {
    List<HistorialEvento> findByUserIdOrderByOccurredAtDesc(String userId);
    List<HistorialEvento> findAllByOrderByOccurredAtDesc();
}

