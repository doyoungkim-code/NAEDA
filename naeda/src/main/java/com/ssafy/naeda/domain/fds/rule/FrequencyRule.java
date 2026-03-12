package com.ssafy.naeda.domain.fds.rule;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;
import org.springframework.stereotype.Component;

/**
 * 단시간 다빈도 결제 탐지.
 *
 * recentPaymentCount: 최근 10분 내 결제 횟수 (현재 건 포함).
 * FdsEngine(또는 BE-010)이 PaymentRepository에서 미리 조회하여 Request에 세팅.
 *
 * 점수 산정:
 *   - 5회 이상 → 33점
 *   - 3~4회   → 25점
 *   - 2회 이하 → 0점
 */
@Component
public class FrequencyRule implements FdsRule {

    private static final int THRESHOLD = 3;
    private static final int HIGH_THRESHOLD = 5;

    @Override
    public FdsRuleName ruleName() {
        return FdsRuleName.HIGH_FREQUENCY;
    }

    @Override
    public int evaluate(FdsEvaluationRequest request) {
        int count = request.getRecentPaymentCount();

        if (count >= HIGH_THRESHOLD) {
            return 33;
        }
        if (count >= THRESHOLD) {
            return 25;
        }
        return 0;
    }
}