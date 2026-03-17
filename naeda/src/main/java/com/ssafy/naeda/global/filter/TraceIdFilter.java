package com.ssafy.naeda.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 traceId를 부여하는 필터.
 *
 * 1. 요청마다 UUID 기반 traceId 생성 → MDC에 저장
 * 2. 응답 헤더 X-Trace-Id에 포함 (프론트/디버깅용)
 * 3. 요청 완료 후 MDC 정리 (스레드 풀 오염 방지)
 */
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString().substring(0, 8);  // 8자리로 짧게

        try {
            MDC.put(TRACE_ID_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);  // 스레드 풀 재사용 시 이전 traceId 오염 방지
        }
    }
}