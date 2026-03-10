package com.ssafy.naeda.domain.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResidentIdVerifyResponse {
    private boolean verified;
    private boolean nameMatched;
    private boolean residentNoMatched;
    private String nextAction;
}
