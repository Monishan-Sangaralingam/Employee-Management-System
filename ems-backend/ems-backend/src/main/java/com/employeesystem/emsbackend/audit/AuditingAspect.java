package com.employeesystem.emsbackend.audit;

import com.employeesystem.emsbackend.entity.AuditLog;
import com.employeesystem.emsbackend.repository.UserRepository;
import com.employeesystem.emsbackend.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class AuditingAspect {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuditingAspect(AuditLogService auditLogService, UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object result = null;
        Throwable error = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                writeAudit(pjp, auditable, result, error);
            } catch (Exception ignored) {
                // auditing must never break business logic
            }
        }
    }

    private void writeAudit(ProceedingJoinPoint pjp, Auditable auditable, Object result, Throwable error) {
        Long actorUserId = resolveActorUserId();
        Long entityId = resolveEntityId(pjp, auditable, result);

        String details = auditable.details();
        if (details == null || details.isBlank()) {
            details = buildDetails(pjp, error);
        }

        AuditLog log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setAction(auditable.action());
        log.setEntity(auditable.entity());
        log.setEntityId(entityId);
        log.setTimestamp(LocalDateTime.now());
        log.setDetails(details);

        auditLogService.save(log);
    }

    private Long resolveActorUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String username = auth.getName();
        if (username == null || username.isBlank()) {
            return null;
        }

        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElse(null);
    }

    private Long resolveEntityId(ProceedingJoinPoint pjp, Auditable auditable, Object result) {
        int idx = auditable.entityIdArgIndex();
        Object[] args = pjp.getArgs();
        if (idx >= 0 && args != null && idx < args.length) {
            Object arg = args[idx];
            if (arg instanceof Number n) {
                return n.longValue();
            }
        }

        if (result != null) {
            try {
                Method m = result.getClass().getMethod("getId");
                Object id = m.invoke(result);
                if (id instanceof Number n) {
                    return n.longValue();
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String buildDetails(ProceedingJoinPoint pjp, Throwable error) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        String args = safeArgs(pjp.getArgs());

        if (error == null) {
            return "method=" + method + "; args=" + args;
        }

        String msg = error.getMessage();
        if (msg == null)
            msg = error.getClass().getSimpleName();
        return "method=" + method + "; args=" + args + "; error=" + truncate(msg, 500);
    }

    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(truncate(String.valueOf(args[i]), 200));
        }
        sb.append("]");
        return truncate(sb.toString(), 1000);
    }

    private String truncate(String s, int max) {
        if (s == null)
            return null;
        if (s.length() <= max)
            return s;
        return s.substring(0, max);
    }
}
