package com.tpi.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoryEventRepository extends JpaRepository<HistoryEvent, UUID> {
    List<HistoryEvent> findByUserIdOrderByOccurredAtDesc(String userId);
    List<HistoryEvent> findAllByOrderByOccurredAtDesc();
}

