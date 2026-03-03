package com.ssafy.naeda.domain.point.dto.response;

import com.ssafy.naeda.domain.point.entity.PointHistory;
import com.ssafy.naeda.domain.point.entity.PointType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointHistoryResponse {
    private Long historyId;

    private PointType type;

    private Long amount;

    private Long balanceAfter;

    private String description;

    private LocalDateTime created;

    public static PointHistoryResponse from(PointHistory history) {
        return PointHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .type(history.getType())
                .amount(history.getAmount())
                .balanceAfter(history.getBalanceAfter())
                .description(history.getDescription())
                .created(history.getCreated())
                .build();
    }
}
