package com.ssafy.naeda.domain.fds.service;

import com.ssafy.naeda.domain.fds.dto.response.AiModelVersionResponse;
import com.ssafy.naeda.domain.fds.dto.response.FdsVersionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FdsVersionServiceTest {

    @InjectMocks
    private FdsVersionService fdsVersionService;

    @Mock
    private RestClient aiRestClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fdsVersionService, "featureVersion", "fds-feature-v1");
        ReflectionTestUtils.setField(fdsVersionService, "ruleVersion", "fds-rule-v1");
    }

    @Test
    @DisplayName("AI가 제공한 버전 정보를 기반으로 현재 FDS 버전을 반환한다")
    void getCurrentVersion_returnsMergedVersionInfo() {
        AiModelVersionResponse aiResponse = mock(AiModelVersionResponse.class);
        given(aiResponse.getFeatureVersion()).willReturn("fds-feature-v1");
        given(aiResponse.getRuleVersion()).willReturn("fds-rule-v1");
        given(aiResponse.getModelVersion()).willReturn("rule-only-v1");
        given(aiResponse.getAlgorithm()).willReturn("RULE_ENGINE");
        given(aiResponse.getArtifactPath()).willReturn("N/A");
        given(aiResponse.getUpdatedAt()).willReturn("2026-03-09T00:00:00");

        given(aiRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("/internal/v1/model/version")).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(AiModelVersionResponse.class)).willReturn(aiResponse);

        FdsVersionResponse result = fdsVersionService.getCurrentVersion();

        assertThat(result.getFeatureVersion()).isEqualTo("fds-feature-v1");
        assertThat(result.getRuleVersion()).isEqualTo("fds-rule-v1");
        assertThat(result.getModelVersion()).isEqualTo("rule-only-v1");
        assertThat(result.getAlgorithm()).isEqualTo("RULE_ENGINE");
    }
}
