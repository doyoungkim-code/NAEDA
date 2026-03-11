package com.ssafy.naeda.domain.fds.rule;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FrequencyRuleTest {

    private final FrequencyRule rule = new FrequencyRule();

    private FdsEvaluationRequest request(int recentCount) {
        return FdsEvaluationRequest.builder()
                .userNo(1L).storeId(10L).amount(10_000L)
                .paymentTime(LocalDateTime.now())
                .recentPaymentCount(recentCount).dailyAverageAmount(10_000L)
                .build();
    }

    @Test
    @DisplayName("ruleName은 HIGH_FREQUENCY")
    void ruleName() {
        assertThat(rule.ruleName()).isEqualTo(FdsRuleName.HIGH_FREQUENCY);
    }

    @Test
    @DisplayName("2회 이하면 0점")
    void belowThreshold_returnsZero() {
        assertThat(rule.evaluate(request(0))).isZero();
        assertThat(rule.evaluate(request(1))).isZero();
        assertThat(rule.evaluate(request(2))).isZero();
    }

    @Test
    @DisplayName("3~4회면 25점")
    void midFrequency() {
        assertThat(rule.evaluate(request(3))).isEqualTo(25);
        assertThat(rule.evaluate(request(4))).isEqualTo(25);
    }

    @Test
    @DisplayName("5회 이상이면 33점")
    void highFrequency() {
        assertThat(rule.evaluate(request(5))).isEqualTo(33);
        assertThat(rule.evaluate(request(10))).isEqualTo(33);
    }
}
