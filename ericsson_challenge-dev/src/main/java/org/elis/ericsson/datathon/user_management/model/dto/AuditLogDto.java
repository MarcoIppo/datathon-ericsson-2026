package org.elis.ericsson.datathon.user_management.model.dto;

import lombok.Builder;
import lombok.Getter;
import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;

import java.time.Instant;

@Getter
@Builder
public class AuditLogDto {

    private Long id;
    private String userEmail;
    private String action;
    private String entityType;
    private String entityId;
    private AuditLog.Outcome outcome;
    private String ipAddress;
    private Instant timestamp;

    public static AuditLogDto from(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .userEmail(log.getUserEmail())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .outcome(log.getOutcome())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .build();
    }
}
