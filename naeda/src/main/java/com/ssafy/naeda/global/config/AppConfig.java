package com.ssafy.naeda.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5)); // 연결 타임아웃 5초
        factory.setReadTimeout(Duration.ofSeconds(10)); // 응답 대기 타임아웃 10초
        return new RestTemplate(factory);
    }
}
