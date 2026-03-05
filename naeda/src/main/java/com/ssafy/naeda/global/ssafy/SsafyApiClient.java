package com.ssafy.naeda.global.ssafy;

import com.ssafy.naeda.global.exception.SsafyApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * SSAFY 금융 API 호출 전담 클라이언트.
 *
 * 모든 SSAFY API 호출은 이 클래스를 통해 이루어진다.
 * 응답 Header의 responseCode가 H0000이 아니면 SsafyApiException을 던진다.
 *
 * 사용 예시:
 *   Map<String, Object> body = new HashMap<>();
 *   body.put("Header", headerFactory.create("inquireDemandDepositAccountList", userKey));
 *   Map<String, Object> response = ssafyApiClient.post(BASE_URL + "/inquireDemandDepositAccountList", body);
 *   List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.get("REC");
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SsafyApiClient {

    private static final String SUCCESS_CODE = "H0000";

    @Value("${ssafy.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * SSAFY API POST 호출.
     *
     * @param path  baseUrl 이후의 경로 (예: "/edu/demandDeposit/inquireDemandDepositAccountList")
     * @param body  요청 body (Header 포함)
     * @return      응답 전체 Map (Header + REC)
     * @throws SsafyApiException SSAFY 응답 코드가 H0000이 아닐 때
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String path, Map<String, Object> body) {
        String url = baseUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.debug("[SsafyApiClient] POST {} | body={}", url, body);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            validateResponse(response, url);

            log.debug("[SsafyApiClient] 응답 성공 {} | response={}", url, response);
            return response;

        } catch (SsafyApiException e) {
            throw e;  // 이미 가공된 예외는 그대로 전파
        } catch (RestClientException e) {
            log.error("[SsafyApiClient] 네트워크 오류 url={}", url, e);
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
     *
     * 사용 예시:
     *   Map<String, Object> body = ssafyApiClient.buildBody(header, "accountNo", "0016174648358792");
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