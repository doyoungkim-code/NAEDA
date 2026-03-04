package com.ssafy.naeda.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    @Value("${ai.service-token}")
    private String aiServiceToken;

    @Value("${ai.timeout-seconds:5}")
    private int aiTimeoutSeconds;

    @Bean
    public RestClient aiRestClient() {
        int timeoutMs = aiTimeoutSeconds * 1000;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .baseUrl(aiBaseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Service-Token", aiServiceToken)
                .build();
    }
}
