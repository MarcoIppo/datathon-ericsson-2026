package org.elis.ericsson.datathon.user_management.repository;

import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);
}
