package com.ssafy.naeda.global.config;

import com.ssafy.naeda.global.filter.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 서블릿 필터 등록 설정.
 * TraceIdFilter를 가장 높은 우선순위로 등록하여
 * 모든 요청의 시작 시점부터 traceId가 MDC에 존재하도록 보장한다.
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");    // 모든 요청에 적용
        registration.setOrder(1);              // 최우선 실행 (Spring Security 등보다 먼저)
        return registration;
    }
}