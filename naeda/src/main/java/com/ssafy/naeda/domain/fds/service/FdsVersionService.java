package com.ssafy.naeda.domain.fds.service;

import com.ssafy.naeda.domain.fds.dto.response.AiModelVersionResponse;
import com.ssafy.naeda.domain.fds.dto.response.FdsVersionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class FdsVersionService {

    private final RestClient aiRestClient;

    @Value("${fds.version.feature:fds-feature-v1}")
    private String featureVersion;

    @Value("${fds.version.rule:fds-rule-v1}")
    private String ruleVersion;

    public FdsVersionResponse getCurrentVersion() {
        AiModelVersionResponse aiVersion = aiRestClient.get()
                .uri("/internal/v1/model/version")
                .retrieve()
                .body(AiModelVersionResponse.class);

        if (aiVersion == null) {
            log.warn("AI model version response is null. Falling back to BE-managed version info.");
            return FdsVersionResponse.builder()
                    .featureVersion(featureVersion)
                    .ruleVersion(ruleVersion)
                    .modelVersion("unknown")
                    .algorithm("unknown")
                    .artifactPath("unknown")
                    .updatedAt("unknown")
                    .build();
        }

        return FdsVersionResponse.builder()
                .featureVersion(aiVersion.getFeatureVersion() != null ? aiVersion.getFeatureVersion() : featureVersion)
                .ruleVersion(aiVersion.getRuleVersion() != null ? aiVersion.getRuleVersion() : ruleVersion)
                .modelVersion(aiVersion.getModelVersion())
                .algorithm(aiVersion.getAlgorithm())
                .artifactPath(aiVersion.getArtifactPath())
                .updatedAt(aiVersion.getUpdatedAt())
                .build();
    }
}
