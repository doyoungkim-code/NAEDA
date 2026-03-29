package com.ssafy.naeda.domain.rba.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RbaResult {
    private final AuthLevel authLevel;
    private final Set<AuthMethod> requiredMethods;
    private final boolean blocked;
    private final String reason;
}
