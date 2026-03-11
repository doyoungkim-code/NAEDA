package com.ssafy.naeda.domain.fds.entity;

/**
 * FDS 규칙 이름.
 * FdsLog.triggeredRules (JSONB)에 저장될 값.
 */
public enum FdsRuleName {

    LATE_NIGHT,         // 심야(00:00~06:00) 고액 결제
    HIGH_FREQUENCY,     // 10분 내 3회 이상 연속 결제
    AMOUNT_ANOMALY;     // 일일 평균의 300% 초과 결제
    // LOCATION_ANOMALY  ← 지역 이상은 추후 추가 (Nice-to-have)
}