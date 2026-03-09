package com.ssafy.naeda.domain.fds.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FdsVersionResponse {
    private String featureVersion;
    private String ruleVersion;
    private String modelVersion;
    private String algorithm;
    private String artifactPath;
    private String updatedAt;
}
