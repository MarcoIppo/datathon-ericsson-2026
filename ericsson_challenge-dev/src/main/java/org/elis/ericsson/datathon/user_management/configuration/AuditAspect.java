package org.elis.ericsson.datathon.user_management.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.elis.ericsson.datathon.user_management.model.audit.AuditAction;
import org.elis.ericsson.datathon.user_management.model.entity.AuditLog;
import org.elis.ericsson.datathon.user_management.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(auditAction)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        String action = auditAction.action();
        String userEmail = resolveUserEmail(joinPoint);
        String ipAddress = resolveIpAddress();

        try {
            Object result = joinPoint.proceed();
            persist(action, userEmail, ipAddress, AuditLog.Outcome.SUCCESS, null);
            return result;
        } catch (Throwable ex) {
            persist(action, userEmail, ipAddress, AuditLog.Outcome.FAILURE, ex.getMessage());
            throw ex;
        }
    }

    private void persist(String action, String userEmail, String ipAddress,
                         AuditLog.Outcome outcome, String errorMessage) {
        try {
            auditLogService.save(AuditLog.builder()
                    .action(action)
                    .userEmail(userEmail)
                    .ipAddress(ipAddress)
                    .outcome(outcome)
                    .errorMessage(errorMessage)
                    .timestamp(Instant.now())
                    .build());
        } catch (Exception e) {
            logger.error("Failed to persist audit log for action {}: {}", action, e.getMessage());
        }
    }

    private String resolveUserEmail(ProceedingJoinPoint joinPoint) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {}

        // Fallback: estrae email dall'argomento se disponibile (es. LoginDto, SignUpRequestDto)
        try {
            for (Object arg : joinPoint.getArgs()) {
                java.lang.reflect.Method getEmail = arg.getClass().getMethod("getEmail");
                String email = (String) getEmail.invoke(arg);
                if (email != null && !email.isBlank()) return email;
            }
        } catch (Exception ignored) {}

        return "anonymous";
    }

    private String resolveIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                return (forwarded != null && !forwarded.isBlank())
                        ? forwarded.split(",")[0].trim()
                        : request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
