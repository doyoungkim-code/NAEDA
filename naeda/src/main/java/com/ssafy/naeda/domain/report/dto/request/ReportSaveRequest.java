package com.ssafy.naeda.domain.report.dto.request;

import com.ssafy.naeda.domain.report.entity.LocalGrade;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "소비 리포트 저장 요청")
public class ReportSaveRequest {

    @NotNull
    @Schema(description = "사용자 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userNo;

    @NotNull
    @Schema(description = "리포트 기간 유형", example = "WEEKLY", requiredMode = Schema.RequiredMode.REQUIRED)
    private PeriodType periodType;

    @NotNull
    @Schema(description = "기간 시작일", example = "2026-02-24", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate periodStart;

    @NotNull
    @Schema(description = "기간 종료일", example = "2026-03-02", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate periodEnd;

    @Schema(description = "카테고리별 지출 내역", example = "{\"식비\": 320000, \"카페\": 85000}")
    private Map<String, Long> categoryBreakdown;

    @Schema(description = "총 지출 금액", example = "405000")
    private Long totalSpending;

    @Schema(description = "지역 소비 금액", example = "200000")
    private Long localSpending;

    @Schema(description = "지역 소비 비율 (0.0~1.0)", example = "0.49")
    private Float localRatio;

    @Schema(description = "지역 소비 등급", example = "B")
    private LocalGrade localGrade;

    @Schema(description = "AI 인사이트 목록", example = "[\"카페 지출이 전주 대비 20% 증가했어요\"]")
    private List<String> insights;
}
