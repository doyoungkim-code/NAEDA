package com.ssafy.naeda.domain.pay.entity;

import java.util.Map;
import java.util.Set;

public enum PayRequestStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    BLOCKED,
    PAUSED;

    // 각 상태에서 전이 가능한 상태 목록
    private static final Map<PayRequestStatus, Set<PayRequestStatus>> VALID_TRANSITIONS = Map.of(
            PENDING, Set.of(PROCESSING, FAILED),
            PROCESSING, Set.of(SUCCESS, FAILED, BLOCKED, PAUSED),
            PAUSED, Set.of(PROCESSING, FAILED)
            // SUCCESS, FAILED, BLOCKED → 최종 상태 (전이 불가)
    );


    /**
     * 현재 상태에서 target 상태로 전이 가능한지 검증
     */
    public boolean canTransitionTo(PayRequestStatus target) {
        Set<PayRequestStatus> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
