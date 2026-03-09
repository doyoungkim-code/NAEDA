package com.ssafy.naeda.domain.fds.dto.response;

import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.entity.FdsLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FdsLogResponse {

    private final Long fdsId;
    private final Long paymentId;
    private final Long userNo;
    private final int anomalyScore;
    private final List<String> triggeredRules;
    private final FdsAction actionTaken;
    private final Boolean userConfirmed;
    private final LocalDateTime detected;

    public static FdsLogResponse from(FdsLog log) {
        return FdsLogResponse.builder()
                .fdsId(log.getFdsId())
                .paymentId(log.getPaymentId())
                .userNo(log.getUserNo())
                .anomalyScore(log.getAnomalyScore())
                .triggeredRules(log.getTriggeredRules())
                .actionTaken(log.getActionTaken())
                .userConfirmed(log.getUserConfirmed())
                .detected(log.getDetected())
                .build();
    }
}
