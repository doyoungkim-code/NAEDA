package com.ssafy.naeda.domain.fds.dto.response;

import com.ssafy.naeda.domain.fds.entity.FdsAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FdsEvaluationResult {

    private final int anomalyScore;
    private final List<String> triggeredRules;
    private final FdsAction action;

    public boolean isPaymentAllowed() {
        return action != FdsAction.BLOCK;
    }

    public boolean requiresUserConfirmation() {
        return action == FdsAction.PAUSE;
    }
}
