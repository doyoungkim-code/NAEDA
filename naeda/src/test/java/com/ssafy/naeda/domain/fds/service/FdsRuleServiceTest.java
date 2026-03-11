package com.ssafy.naeda.domain.fds.service;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.dto.response.FdsEvaluationResult;
import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.entity.FdsLog;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;
import com.ssafy.naeda.domain.fds.repository.FdsLogRepository;
import com.ssafy.naeda.domain.fds.rule.FdsRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FdsRuleServiceTest {

    @Mock
    private FdsLogRepository fdsLogRepository;

    @InjectMocks
    private FdsRuleService fdsRuleService;

    private FdsEvaluationRequest normalRequest() {
        return FdsEvaluationRequest.builder()
                .userNo(1L).storeId(10L).amount(10_000L)
                .paymentTime(LocalDateTime.of(2026, 3, 11, 14, 0))
                .recentPaymentCount(1).dailyAverageAmount(20_000L)
                .build();
    }

    @Test
    @DisplayName("모든 룰이 0점이면 NONE 반환")
    void allRulesZero_returnsNone() {
        FdsRule zeroRule = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.LATE_NIGHT; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 0; }
        };
        FdsRuleService service = new FdsRuleService(List.of(zeroRule), fdsLogRepository);

        FdsEvaluationResult result = service.evaluate(normalRequest());

        assertThat(result.getAnomalyScore()).isZero();
        assertThat(result.getTriggeredRules()).isEmpty();
        assertThat(result.getAction()).isEqualTo(FdsAction.NONE);
    }

    @Test
    @DisplayName("점수 합산 30~59이면 ALERT")
    void scoreAlert() {
        FdsRule rule = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.LATE_NIGHT; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 33; }
        };
        FdsRuleService service = new FdsRuleService(List.of(rule), fdsLogRepository);

        FdsEvaluationResult result = service.evaluate(normalRequest());

        assertThat(result.getAnomalyScore()).isEqualTo(33);
        assertThat(result.getAction()).isEqualTo(FdsAction.ALERT);
        assertThat(result.getTriggeredRules()).containsExactly("LATE_NIGHT");
    }

    @Test
    @DisplayName("점수 합산 60~79이면 PAUSE")
    void scorePause() {
        FdsRule rule1 = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.LATE_NIGHT; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 33; }
        };
        FdsRule rule2 = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.HIGH_FREQUENCY; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 33; }
        };
        FdsRuleService service = new FdsRuleService(List.of(rule1, rule2), fdsLogRepository);

        FdsEvaluationResult result = service.evaluate(normalRequest());

        assertThat(result.getAnomalyScore()).isEqualTo(66);
        assertThat(result.getAction()).isEqualTo(FdsAction.PAUSE);
        assertThat(result.getTriggeredRules()).containsExactly("LATE_NIGHT", "HIGH_FREQUENCY");
    }

    @Test
    @DisplayName("점수 합산 80 이상이면 BLOCK")
    void scoreBlock() {
        FdsRule rule1 = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.LATE_NIGHT; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 33; }
        };
        FdsRule rule2 = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.HIGH_FREQUENCY; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 33; }
        };
        FdsRule rule3 = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.AMOUNT_ANOMALY; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 25; }
        };
        FdsRuleService service = new FdsRuleService(List.of(rule1, rule2, rule3), fdsLogRepository);

        FdsEvaluationResult result = service.evaluate(normalRequest());

        assertThat(result.getAnomalyScore()).isEqualTo(91);
        assertThat(result.getAction()).isEqualTo(FdsAction.BLOCK);
        assertThat(result.getTriggeredRules()).hasSize(3);
    }

    @Test
    @DisplayName("점수 100 초과 시 100으로 cap")
    void scoreCappedAt100() {
        FdsRule rule = new FdsRule() {
            @Override public FdsRuleName ruleName() { return FdsRuleName.LATE_NIGHT; }
            @Override public int evaluate(FdsEvaluationRequest r) { return 120; }
        };
        FdsRuleService service = new FdsRuleService(List.of(rule), fdsLogRepository);

        FdsEvaluationResult result = service.evaluate(normalRequest());

        assertThat(result.getAnomalyScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("saveLog로 FdsLog가 정상 저장된다")
    void saveLog() {
        when(fdsLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FdsEvaluationResult result = new FdsEvaluationResult(25, List.of("HIGH_FREQUENCY"), FdsAction.NONE);
        fdsRuleService.saveLog(55L, 7L, result);

        ArgumentCaptor<FdsLog> captor = ArgumentCaptor.forClass(FdsLog.class);
        verify(fdsLogRepository).save(captor.capture());

        FdsLog saved = captor.getValue();
        assertThat(saved.getPaymentId()).isEqualTo(55L);
        assertThat(saved.getUserNo()).isEqualTo(7L);
        assertThat(saved.getAnomalyScore()).isEqualTo(25);
        assertThat(saved.getTriggeredRules()).containsExactly("HIGH_FREQUENCY");
        assertThat(saved.getActionTaken()).isEqualTo(FdsAction.NONE);
    }
}
