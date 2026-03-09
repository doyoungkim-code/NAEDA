package com.ssafy.naeda.domain.fds.rule;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AmountAnomalyRuleTest {

    private final AmountAnomalyRule rule = new AmountAnomalyRule();

    private FdsEvaluationRequest request(long amount, long dailyAvg) {
        return FdsEvaluationRequest.builder()
                .userNo(1L).storeId(10L).amount(amount)
                .paymentTime(LocalDateTime.now())
                .recentPaymentCount(0).dailyAverageAmount(dailyAvg)
                .build();
    }

    @Test
    @DisplayName("ruleName은 AMOUNT_ANOMALY")
    void ruleName() {
        assertThat(rule.ruleName()).isEqualTo(FdsRuleName.AMOUNT_ANOMALY);
    }

    @Test
    @DisplayName("일평균 0이면 판단 불가 → 0점")
    void noHistory_returnsZero() {
        assertThat(rule.evaluate(request(100_000L, 0))).isZero();
    }

    @Test
    @DisplayName("300% 이하면 0점")
    void withinNormal() {
        assertThat(rule.evaluate(request(30_000L, 10_000L))).isZero();  // 300% 딱
        assertThat(rule.evaluate(request(20_000L, 10_000L))).isZero();  // 200%
    }

    @Test
    @DisplayName("300% 초과 500% 이하면 25점")
    void alertLevel() {
        assertThat(rule.evaluate(request(30_001L, 10_000L))).isEqualTo(25);
        assertThat(rule.evaluate(request(50_000L, 10_000L))).isEqualTo(25);  // 500% 딱
    }

    @Test
    @DisplayName("500% 초과면 33점")
    void dangerLevel() {
        assertThat(rule.evaluate(request(50_001L, 10_000L))).isEqualTo(33);
        assertThat(rule.evaluate(request(100_000L, 10_000L))).isEqualTo(33);
    }
}
