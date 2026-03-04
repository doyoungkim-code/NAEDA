package com.ssafy.naeda.domain.point.dto.response;

import com.ssafy.naeda.domain.point.entity.PointHistory;
import com.ssafy.naeda.domain.point.entity.PointType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "포인트 이력 응답")
public class PointHistoryResponse {
    @Schema(description = "이력 ID", example = "101")
    private Long historyId;

    @Schema(description = "이력 유형", example = "EARN")
    private PointType type;

    @Schema(description = "변동 포인트", example = "1000")
    private Long amount;

    @Schema(description = "처리 후 잔액", example = "2500")
    private Long balanceAfter;

    @Schema(description = "설명", example = "출석 보상")
    private String description;

    @Schema(description = "생성 시각", example = "2026-03-04T10:15:30")
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
