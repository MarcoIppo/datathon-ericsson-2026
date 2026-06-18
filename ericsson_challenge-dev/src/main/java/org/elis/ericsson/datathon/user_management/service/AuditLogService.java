package org.elis.ericsson.datathon.user_management.service;

import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void save(AuditLog log);

    Page<AuditLog> findAll(Pageable pageable);

    Page<AuditLog> findByEmail(String email, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);
}
