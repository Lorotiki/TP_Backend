package com.tpi.history.repository;

import com.tpi.history.entity.UserOperationView;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserOperationViewRepository extends JpaRepository<UserOperationView, UUID> {
}

