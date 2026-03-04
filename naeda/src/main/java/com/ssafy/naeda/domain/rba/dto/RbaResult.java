package com.ssafy.naeda.domain.rba.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RbaResult {
    private final AuthLevel authLevel;
    private final String reason;  // "금액 5만원 이상", "유사도 경계 구간" 등
}