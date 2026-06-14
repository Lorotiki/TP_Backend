package com.tpi.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserOperationViewRepository extends JpaRepository<UserOperationView, UUID> {
}

