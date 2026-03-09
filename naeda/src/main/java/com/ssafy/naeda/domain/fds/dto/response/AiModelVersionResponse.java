package com.ssafy.naeda.domain.fds.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiModelVersionResponse {

    @JsonProperty("featureVersion")
    private String featureVersion;

    @JsonProperty("ruleVersion")
    private String ruleVersion;

    @JsonProperty("modelVersion")
    private String modelVersion;

    private String algorithm;

    @JsonProperty("artifactPath")
    private String artifactPath;

    @JsonProperty("updatedAt")
    private String updatedAt;
}
