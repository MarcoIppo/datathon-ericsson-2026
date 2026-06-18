package org.elis.ericsson.datathon.user_management.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Persistent record of a user action for audit and compliance purposes.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_user_email", columnList = "user_email"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    public enum Outcome { SUCCESS, FAILURE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(nullable = false)
    private Instant timestamp;
}
