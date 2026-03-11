package com.ssafy.naeda.domain.fds.rule;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LateNightRuleTest {

    private final LateNightRule rule = new LateNightRule();

    private FdsEvaluationRequest request(int hour, long amount) {
        return FdsEvaluationRequest.builder()
                .userNo(1L).storeId(10L).amount(amount)
                .paymentTime(LocalDateTime.of(2026, 3, 11, hour, 0))
                .recentPaymentCount(0).dailyAverageAmount(0)
                .build();
    }

    @Test
    @DisplayName("ruleName은 LATE_NIGHT")
    void ruleName() {
        assertThat(rule.ruleName()).isEqualTo(FdsRuleName.LATE_NIGHT);
    }

    @Test
    @DisplayName("낮 시간대(06시 이후)면 0점")
    void daytime_returnsZero() {
        assertThat(rule.evaluate(request(6, 100_000L))).isZero();
        assertThat(rule.evaluate(request(12, 100_000L))).isZero();
        assertThat(rule.evaluate(request(23, 100_000L))).isZero();
    }

    @Test
    @DisplayName("심야 + 5만원 이상이면 33점")
    void lateNight_highAmount() {
        assertThat(rule.evaluate(request(2, 50_000L))).isEqualTo(33);
        assertThat(rule.evaluate(request(0, 100_000L))).isEqualTo(33);
    }

    @Test
    @DisplayName("심야 + 3만원 이상 5만원 미만이면 20점")
    void lateNight_midAmount() {
        assertThat(rule.evaluate(request(3, 30_000L))).isEqualTo(20);
        assertThat(rule.evaluate(request(1, 49_999L))).isEqualTo(20);
    }

    @Test
    @DisplayName("심야 + 3만원 미만이면 10점")
    void lateNight_lowAmount() {
        assertThat(rule.evaluate(request(4, 29_999L))).isEqualTo(10);
        assertThat(rule.evaluate(request(5, 1_000L))).isEqualTo(10);
    }
}
