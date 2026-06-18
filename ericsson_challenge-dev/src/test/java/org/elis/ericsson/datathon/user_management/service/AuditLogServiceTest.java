package org.elis.ericsson.datathon.user_management.service;

import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.elis.ericsson.datathon.user_management.repository.AuditLogRepository;
import org.elis.ericsson.datathon.user_management.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository);
    }

    /**
     * Scenario: salvataggio di un log di audit.
     * Precondizioni: AuditLog valido.
     * Risultato atteso: repository.save() invocato con il log corretto.
     */
    @Test
    void shouldPersistLog_whenSaveCalled() {
        AuditLog log = AuditLog.builder()
                .action("USER_LOGIN").userEmail("u@test.com")
                .outcome(AuditLog.Outcome.SUCCESS).timestamp(Instant.now()).build();

        auditLogService.save(log);

        verify(auditLogRepository).save(log);
    }

    /**
     * Scenario: recupero paginato di tutti i log.
     * Precondizioni: repository restituisce una pagina con un elemento.
     * Risultato atteso: Page con 1 elemento.
     */
    @Test
    void shouldReturnPagedLogs_whenFindAllCalled() {
        PageRequest pageable = PageRequest.of(0, 20);
        AuditLog log = AuditLog.builder().action("USER_LOGIN").build();
        when(auditLogRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLog> result = auditLogService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Scenario: filtro per email utente.
     * Precondizioni: email fornita.
     * Risultato atteso: viene invocata la query per email, non findAll.
     */
    @Test
    void shouldFilterByEmail_whenEmailProvided() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findByUserEmailContainingIgnoreCase("u@test.com", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        auditLogService.findByEmail("u@test.com", pageable);

        verify(auditLogRepository).findByUserEmailContainingIgnoreCase("u@test.com", pageable);
        verify(auditLogRepository, never()).findAll(pageable);
    }
}
