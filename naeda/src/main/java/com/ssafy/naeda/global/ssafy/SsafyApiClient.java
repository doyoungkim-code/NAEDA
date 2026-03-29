package com.ssafy.naeda.global.ssafy;

import com.ssafy.naeda.global.exception.SsafyApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * SSAFY 금융 API 호출 전담 클라이언트.
 *
 * Resilience4j Retry + CircuitBreaker를 적용하여
 * 네트워크 오류 시 자동 재시도, 연속 장애 시 서킷 차단을 수행한다.
 *
 * 데코레이터 순서: CircuitBreaker(바깥) → Retry(안쪽) → 실제 HTTP 호출
 * - CB OPEN 상태에서는 retry 자체를 수행하지 않음
 * - SsafyApiException(비즈니스 오류)은 재시도하지 않음
 */
@Slf4j
@Component
public class SsafyApiClient {

    private static final String SUCCESS_CODE = "H0000";

    @Value("${ssafy.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public SsafyApiClient(RestTemplate restTemplate,
                          CircuitBreakerRegistry circuitBreakerRegistry,
                          RetryRegistry retryRegistry) {
        this.restTemplate   = restTemplate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("ssafyApi");
        this.retry          = retryRegistry.retry("ssafyApi");
    }

    /**
     * SSAFY API POST 호출 (Retry + CircuitBreaker 적용).
     *
     * @param path  baseUrl 이후의 경로 (예: "/edu/demandDeposit/inquireDemandDepositAccountList")
     * @param body  요청 body (Header 포함)
     * @return      응답 전체 Map (Header + REC)
     * @throws SsafyApiException                    SSAFY 응답 코드가 H0000이 아닐 때
     * @throws io.github.resilience4j.circuitbreaker.CallNotPermittedException  CB OPEN 상태일 때
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String path, Map<String, Object> body) {
        String url = baseUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.debug("[SsafyApiClient] POST {}", url);

        // 실제 HTTP 호출을 Supplier로 감싼다
        Supplier<Map<String, Object>> apiCall = () -> {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );
            Map<String, Object> response = responseEntity.getBody();
            validateResponse(response, url);
            log.debug("[SsafyApiClient] 응답 성공 {}", url);
            return response;
        };

        // CircuitBreaker(바깥) → Retry(안쪽) → 실제 호출
        Supplier<Map<String, Object>> decorated =
                CircuitBreaker.decorateSupplier(circuitBreaker,
                        Retry.decorateSupplier(retry, apiCall));

        try {
            return decorated.get();
        } catch (SsafyApiException e) {
            throw e;  // 비즈니스 오류는 그대로 전파
        } catch (RestClientException e) {
            log.error("[SsafyApiClient] Retry 소진 후 최종 실패 url={}", url, e);
            throw new SsafyApiException("NETWORK_ERROR", "SSAFY API 네트워크 오류: " + e.getMessage());
        }
    }

    /**
     * 응답 Header의 responseCode 검증.
     * H0000이 아니면 SsafyApiException을 던진다.
     */
    @SuppressWarnings("unchecked")
    private void validateResponse(Map<String, Object> response, String url) {
        if (response == null) {
            throw new SsafyApiException("NULL_RESPONSE", "SSAFY API 응답이 null입니다: " + url);
        }

        Map<String, Object> responseHeader = (Map<String, Object>) response.get("Header");
        if (responseHeader == null) {
            throw new SsafyApiException("NO_HEADER", "SSAFY API 응답에 Header가 없습니다: " + url);
        }

        String responseCode    = (String) responseHeader.get("responseCode");
        String responseMessage = (String) responseHeader.getOrDefault("responseMessage", "알 수 없는 오류");

        if (!SUCCESS_CODE.equals(responseCode)) {
            log.warn("[SsafyApiClient] 오류 응답 url={} | code={} | message={}", url, responseCode, responseMessage);
            throw new SsafyApiException(responseCode, responseMessage);
        }
    }

    /**
     * Header + 추가 필드를 합쳐 request body를 만드는 헬퍼.
     */
    public Map<String, Object> buildBody(Map<String, Object> header, Object... keyValues) {
        Map<String, Object> body = new HashMap<>();
        body.put("Header", header);

        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues는 짝수 개여야 합니다 (key, value 쌍)");
        }

        for (int i = 0; i < keyValues.length; i += 2) {
            body.put((String) keyValues[i], keyValues[i + 1]);
        }

        return body;
    }
}