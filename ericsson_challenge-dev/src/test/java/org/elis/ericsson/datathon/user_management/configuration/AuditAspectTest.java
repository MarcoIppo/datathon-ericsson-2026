package org.elis.ericsson.datathon.user_management.configuration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.elis.ericsson.datathon.user_management.model.audit.AuditAction;
import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.elis.ericsson.datathon.user_management.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private AuditAction auditAction;

    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditLogService);
        SecurityContextHolder.clearContext();
    }

    /**
     * Scenario: metodo annotato eseguito senza eccezioni.
     * Precondizioni: utente autenticato nel SecurityContext.
     * Risultato atteso: AuditLogService.save() chiamato con outcome=SUCCESS e email utente.
     */
    @Test
    void shouldSaveSuccessLog_whenMethodCompletesNormally() throws Throwable {
        when(auditAction.action()).thenReturn("USER_LOGIN");
        when(joinPoint.proceed()).thenReturn("ok");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", null, java.util.List.of()));

        auditAspect.audit(joinPoint, auditAction);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo(AuditLog.Outcome.SUCCESS);
        assertThat(captor.getValue().getUserEmail()).isEqualTo("user@test.com");
        assertThat(captor.getValue().getAction()).isEqualTo("USER_LOGIN");
    }

    /**
     * Scenario: metodo annotato lancia eccezione.
     * Precondizioni: joinPoint.proceed() lancia RuntimeException.
     * Risultato atteso: outcome=FAILURE, eccezione ripropagata.
     */
    @Test
    void shouldSaveFailureLog_whenMethodThrowsException() throws Throwable {
        when(auditAction.action()).thenReturn("USER_LOGIN");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("bad credentials"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditAction))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo(AuditLog.Outcome.FAILURE);
    }

    /**
     * Scenario: SecurityContext vuoto (nessun utente autenticato).
     * Precondizioni: SecurityContextHolder non contiene autenticazione.
     * Risultato atteso: userEmail="anonymous".
     */
    @Test
    void shouldUseAnonymous_whenNoAuthentication() throws Throwable {
        when(auditAction.action()).thenReturn("USER_LOGIN");
        when(joinPoint.proceed()).thenReturn("ok");

        auditAspect.audit(joinPoint, auditAction);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());
        assertThat(captor.getValue().getUserEmail()).isEqualTo("anonymous");
    }

    /**
     * Scenario: AuditLogService.save() lancia eccezione.
     * Precondizioni: il metodo principale va a buon fine, ma il salvataggio del log fallisce.
     * Risultato atteso: il metodo principale completa ugualmente (nessuna eccezione propagata).
     */
    @Test
    void shouldNotBlockFlow_whenAuditSaveFails() throws Throwable {
        when(auditAction.action()).thenReturn("USER_LOGIN");
        when(joinPoint.proceed()).thenReturn("ok");
        doThrow(new RuntimeException("DB down")).when(auditLogService).save(any());

        // deve completare senza lanciare eccezioni
        Object result = auditAspect.audit(joinPoint, auditAction);
        assertThat(result).isEqualTo("ok");
    }
}
