package org.elis.ericsson.datathon.user_management.repository;

import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    long countByActionAndOutcomeAndTimestampAfter(String action, AuditLog.Outcome outcome, Instant since);

    long countByActionAndTimestampAfter(String action, Instant since);

    @Query(value = "SELECT EXTRACT(HOUR FROM a.timestamp) as hr, COUNT(a) FROM AuditLog a " +
           "WHERE a.action = 'USER_LOGIN' AND a.outcome = 'SUCCESS' AND a.timestamp >= :since " +
           "GROUP BY EXTRACT(HOUR FROM a.timestamp)")
    List<Object[]> countLoginsByHour(@Param("since") Instant since);

    @Query(value = "SELECT CAST(a.timestamp AS date), COUNT(a) FROM AuditLog a " +
           "WHERE a.action = 'USER_LOGIN' AND a.timestamp >= :since " +
           "GROUP BY CAST(a.timestamp AS date) ORDER BY CAST(a.timestamp AS date)")
    List<Object[]> countLoginsByDay(@Param("since") Instant since);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> countByAction();

    @Query("SELECT a.userEmail, COUNT(a) FROM AuditLog a GROUP BY a.userEmail ORDER BY COUNT(a) DESC")
    List<Object[]> topUsersByActivity(Pageable pageable);
}
