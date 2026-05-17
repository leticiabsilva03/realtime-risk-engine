package com.fraudengine.domain.repository;

import com.fraudengine.infrastructure.persistence.AuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditWriteRepository extends JpaRepository<AuditEntity, UUID> {

}