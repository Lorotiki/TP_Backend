package com.tpi.history.repository;

import com.tpi.history.entity.OperacionUsuarioView;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OperacionUsuarioViewRepository extends JpaRepository<OperacionUsuarioView, UUID> {
}

