package org.elis.ericsson.datathon.user_management.controller;

import org.elis.ericsson.datathon.user_management.model.dto.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/audit")
public interface AuditLogController {

    @GetMapping
    ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);
}
