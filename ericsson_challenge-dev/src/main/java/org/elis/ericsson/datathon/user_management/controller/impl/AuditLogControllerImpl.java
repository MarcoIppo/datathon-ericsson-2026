package org.elis.ericsson.datathon.user_management.controller.impl;

import org.elis.ericsson.datathon.user_management.controller.AuditLogController;
import org.elis.ericsson.datathon.user_management.model.dto.AuditLogDto;
import org.elis.ericsson.datathon.user_management.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogControllerImpl implements AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogControllerImpl(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(String email, String action, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogDto> result;
        if (email != null && !email.isBlank()) {
            result = auditLogService.findByEmail(email, pageable).map(AuditLogDto::from);
        } else if (action != null && !action.isBlank()) {
            result = auditLogService.findByAction(action, pageable).map(AuditLogDto::from);
        } else {
            result = auditLogService.findAll(pageable).map(AuditLogDto::from);
        }

        return ResponseEntity.ok(result);
    }
}
