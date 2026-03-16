package com.ssafy.naeda.global.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 모든 @RestController 메서드에 대해 감사 로그를 자동 기록하는 AOP.
 *
 * 기록 항목: HTTP Method, URI, userId(SecurityContext), 응답 상태, 소요 시간
 * 로그는 AUDIT 로거로 출력되어 audit.log 파일에 별도 저장된다.
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    /**
     * @RestController 하위 모든 public 메서드에 적용
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String userId = extractUserId();

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            int status = extractStatus(result);

            AUDIT.info("[AUDIT] {} {} | user={} | {} | {}ms",
                    method, uri, userId, status, elapsed);

            return result;

        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;

            AUDIT.warn("[AUDIT] {} {} | user={} | EXCEPTION: {} | {}ms",
                    method, uri, userId,
                    e.getClass().getSimpleName() + ": " + e.getMessage(),
                    elapsed);

            throw e;
        }
    }

    /**
     * SecurityContext에서 인증된 사용자 ID 추출
     */
    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }

    /**
     * 현재 HTTP 요청 가져오기
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * 응답에서 HTTP 상태 코드 추출
     */
    private int extractStatus(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return 200;
    }
}