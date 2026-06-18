package org.elis.ericsson.datathon.user_management.service.impl;

import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.elis.ericsson.datathon.user_management.repository.AuditLogRepository;
import org.elis.ericsson.datathon.user_management.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void save(AuditLog log) {
        auditLogRepository.save(log);
    }

    @Override
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Override
    public Page<AuditLog> findByEmail(String email, Pageable pageable) {
        return auditLogRepository.findByUserEmailContainingIgnoreCase(email, pageable);
    }

    @Override
    public Page<AuditLog> findByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
}
