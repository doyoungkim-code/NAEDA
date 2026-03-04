package com.ssafy.naeda.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter{

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri= request.getRequestURI();
        String remote = request.getRemoteAddr();
        String method = request.getMethod();
        log.info("[REQ] {} {} from {}", method, uri, remote);

        try{
            filterChain.doFilter(request,response);
        }finally {
            log.info("[CALLBACK][OUT] {} {} status={}", method, uri, response.getStatus());
        }
    }
}
