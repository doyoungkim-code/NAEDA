package com.ssafy.naeda.global.ssafy;

import com.ssafy.naeda.global.exception.SsafyApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SsafyApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SsafyApiClient ssafyApiClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ssafyApiClient, "baseUrl",
                "https://finopenapi.ssafy.io/ssafy/api/v1");
    }

    @Test
    @DisplayName("H0000 응답이면 응답 Map을 그대로 반환한다")
    void post_successResponse_returnsBody() {
        Map<String, Object> responseBody = makeSuccessResponse("inquireDemandDepositAccountList");
        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(responseBody));

        Map<String, Object> result = ssafyApiClient.post(
                "/edu/demandDeposit/inquireDemandDepositAccountList",
                new HashMap<>()
        );

        assertThat(result).isEqualTo(responseBody);
    }

    @Test
    @DisplayName("responseCode가 H0000이 아니면 SsafyApiException을 던진다")
    void post_errorResponseCode_throwsSsafyApiException() {
        Map<String, Object> errorBody = makeErrorResponse("E1001", "계좌를 찾을 수 없습니다");
        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(errorBody));

        assertThatThrownBy(() ->
                ssafyApiClient.post("/edu/demandDeposit/inquireDemandDepositAccount", new HashMap<>())
        )
                .isInstanceOf(SsafyApiException.class)
                .hasMessageContaining("계좌를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("응답 body가 null이면 SsafyApiException을 던진다")
    void post_nullResponse_throwsSsafyApiException() {
        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() ->
                ssafyApiClient.post("/some/path", new HashMap<>())
        ).isInstanceOf(SsafyApiException.class);
    }

    @Test
    @DisplayName("네트워크 오류가 발생하면 SsafyApiException(NETWORK_ERROR)을 던진다")
    void post_networkError_throwsSsafyApiException() {
        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .willThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() ->
                ssafyApiClient.post("/some/path", new HashMap<>())
        )
                .isInstanceOf(SsafyApiException.class)
                .extracting("errorCode")
                .isEqualTo("NETWORK_ERROR");
    }

    @Test
    @DisplayName("buildBody는 Header와 추가 필드를 합쳐서 반환한다")
    void buildBody_combinesHeaderAndFields() {
        Map<String, Object> header = Map.of("apiName", "testApi");

        Map<String, Object> body = ssafyApiClient.buildBody(header,
                "accountNo", "001-12345",
                "startDate", "20260101"
        );

        assertThat(body).containsEntry("Header", header);
        assertThat(body).containsEntry("accountNo", "001-12345");
        assertThat(body).containsEntry("startDate", "20260101");
    }

    @Test
    @DisplayName("buildBody에 홀수 개 keyValues를 넘기면 IllegalArgumentException을 던진다")
    void buildBody_oddKeyValues_throwsException() {
        Map<String, Object> header = Map.of("apiName", "testApi");

        assertThatThrownBy(() -> ssafyApiClient.buildBody(header, "accountNo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private Map<String, Object> makeSuccessResponse(String apiName) {
        Map<String, Object> header = new HashMap<>();
        header.put("responseCode", "H0000");
        header.put("responseMessage", "정상처리 되었습니다.");
        header.put("apiName", apiName);

        Map<String, Object> response = new HashMap<>();
        response.put("Header", header);
        response.put("REC", new HashMap<>());
        return response;
    }

    private Map<String, Object> makeErrorResponse(String code, String message) {
        Map<String, Object> header = new HashMap<>();
        header.put("responseCode", code);
        header.put("responseMessage", message);

        Map<String, Object> response = new HashMap<>();
        response.put("Header", header);
        return response;
    }
}