package com.ssafy.naeda.domain.fds.controller;

import com.ssafy.naeda.domain.fds.dto.response.FdsVersionResponse;
import com.ssafy.naeda.domain.fds.service.FdsVersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FdsInternalControllerTest {

    @InjectMocks
    private FdsInternalController fdsInternalController;

    @Mock
    private FdsVersionService fdsVersionService;

    @Test
    @DisplayName("현재 FDS 버전 정보를 반환한다")
    void getCurrentVersion_returnsVersionPayload() {
        given(fdsVersionService.getCurrentVersion()).willReturn(
                FdsVersionResponse.builder()
                        .featureVersion("fds-feature-v1")
                        .ruleVersion("fds-rule-v1")
                        .modelVersion("rule-only-v1")
                        .algorithm("RULE_ENGINE")
                        .artifactPath("N/A")
                        .updatedAt("2026-03-09T00:00:00")
                        .build()
        );

        ResponseEntity<FdsVersionResponse> response = fdsInternalController.getCurrentVersion();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRuleVersion()).isEqualTo("fds-rule-v1");
        assertThat(response.getBody().getModelVersion()).isEqualTo("rule-only-v1");
    }
}
