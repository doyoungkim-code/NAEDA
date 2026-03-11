package com.ssafy.naeda.domain.fds.rule;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;
import org.springframework.stereotype.Component;

/**
 * 심야 시간대(00:00~06:00) 고액 결제 탐지.
 *
 * 점수 산정 기준:
 *   - 심야 + 5만원 이상  → 33점 (고위험)
 *   - 심야 + 3만원 이상  → 20점 (중위험)
 *   - 심야 + 3만원 미만  → 10점 (저위험, 심야지만 소액)
 *   - 심야 아님           → 0점
 *
 * ※ 금액 임계값은 프로젝트 상황에 맞게 조정 가능.
 */
@Component
public class LateNightRule implements FdsRule {

    private static final int NIGHT_START = 0;   // 00시
    private static final int NIGHT_END = 6;     // 06시
    private static final long HIGH_AMOUNT = 50_000L;
    private static final long MID_AMOUNT = 30_000L;

    @Override
    public FdsRuleName ruleName() {
        return FdsRuleName.LATE_NIGHT;
    }

    @Override
    public int evaluate(FdsEvaluationRequest request) {
        int hour = request.getPaymentTime().getHour();

        // 심야 시간대가 아니면 0점
        if (hour >= NIGHT_END) {
            return 0;
        }

        // 심야 시간대 → 금액에 따라 점수 차등
        if (request.getAmount() >= HIGH_AMOUNT) {
            return 33;
        }
        if (request.getAmount() >= MID_AMOUNT) {
            return 20;
        }
        return 10;  // 심야 소액 — 약간의 점수만
    }
}