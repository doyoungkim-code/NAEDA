package com.ssafy.naeda.domain.fds.rule;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;
import org.springframework.stereotype.Component;

/**
 * 금액 이상 탐지 — 일일 평균 결제 금액의 N배 초과 시 이상 감지.
 *
 * dailyAverageAmount: 사용자의 최근 30일 일일 평균 결제 금액.
 * FdsEngine(또는 BE-010)이 미리 조회하여 Request에 세팅.
 *
 * 점수 산정:
 *   - 일평균 0 (이력 없음) → 0점 (신규 사용자 — 판단 불가)
 *   - 500% 초과           → 33점
 *   - 300% 초과           → 25점
 *   - 300% 이하           → 0점
 */
@Component
public class AmountAnomalyRule implements FdsRule {

    private static final double ALERT_MULTIPLIER = 3.0;     // 300%
    private static final double DANGER_MULTIPLIER = 5.0;    // 500%

    @Override
    public FdsRuleName ruleName() {
        return FdsRuleName.AMOUNT_ANOMALY;
    }

    @Override
    public int evaluate(FdsEvaluationRequest request) {
        long dailyAvg = request.getDailyAverageAmount();

        // 이력이 없으면 판단 불가 → 스킵
        if (dailyAvg <= 0) {
            return 0;
        }

        double ratio = (double) request.getAmount() / dailyAvg;

        if (ratio > DANGER_MULTIPLIER) {
            return 33;
        }
        if (ratio > ALERT_MULTIPLIER) {
            return 25;
        }
        return 0;
    }
}