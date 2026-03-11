package com.ssafy.naeda.domain.fds.service;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.dto.response.FdsEvaluationResult;
import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.entity.FdsLog;
import com.ssafy.naeda.domain.fds.repository.FdsLogRepository;
import com.ssafy.naeda.domain.fds.rule.FdsRule;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FdsRuleService {

    private final List<FdsRule> rules;
    private final FdsLogRepository fdsLogRepository;

    @Transactional
    public FdsEvaluationResult evaluate(Long paymentId, FdsEvaluationRequest request) {
        int totalScore = 0;
        List<String> triggeredRules = new ArrayList<>();

        for (FdsRule rule : rules) {
            int score = rule.evaluate(request);
            if (score > 0) {
                totalScore += score;
                triggeredRules.add(rule.ruleName().name());
            }
        }

        totalScore = Math.min(totalScore, 100);
        FdsAction action = determineAction(totalScore);

        fdsLogRepository.save(FdsLog.builder()
                .paymentId(paymentId)
                .userNo(request.getUserNo())
                .anomalyScore(totalScore)
                .triggeredRules(triggeredRules)
                .actionTaken(action)
                .build());

        log.info("[FDS] userNo={}, paymentId={}, score={}, action={}, rules={}",
                request.getUserNo(), paymentId, totalScore, action, triggeredRules);

        return new FdsEvaluationResult(totalScore, triggeredRules, action);
    }

    @Transactional(readOnly = true)
    public List<FdsLog> getLogsByUserNo(Long userNo) {
        return fdsLogRepository.findByUserNo(userNo);
    }

    @Transactional(readOnly = true)
    public FdsLog getLogByPaymentId(Long paymentId) {
        return fdsLogRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new NotFoundException("해당 결제의 FDS 로그가 없습니다: " + paymentId));
    }

    private FdsAction determineAction(int score) {
        if (score >= 80) return FdsAction.BLOCK;
        if (score >= 60) return FdsAction.PAUSE;
        if (score >= 30) return FdsAction.ALERT;
        return FdsAction.NONE;
    }
}
