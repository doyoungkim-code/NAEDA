package com.ssafy.naeda.domain.report.dto.response;

import com.ssafy.naeda.domain.report.entity.ConsumptionReport;
import com.ssafy.naeda.domain.report.entity.LocalGrade;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(description = "소비 리포트 응답")
public class ReportResponse {

    @Schema(description = "리포트 ID", example = "1")
    private Long reportId;

    @Schema(description = "리포트 기간 유형", example = "WEEKLY")
    private PeriodType periodType;

    @Schema(description = "기간 시작일", example = "2026-02-24")
    private LocalDate periodStart;

    @Schema(description = "기간 종료일", example = "2026-03-02")
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

    @Schema(description = "리포트 생성 시각", example = "2026-03-04T09:30:00")
    private LocalDateTime generated;

    public static ReportResponse from(ConsumptionReport reportResponse) {
        return ReportResponse.builder()
                .reportId(reportResponse.getReportId())
                .periodType(reportResponse.getPeriodType())
                .periodStart(reportResponse.getPeriodStart())
                .periodEnd(reportResponse.getPeriodEnd())
                .categoryBreakdown(reportResponse.getCategoryBreakdown())
                .totalSpending(reportResponse.getTotalSpending())
                .localSpending(reportResponse.getLocalSpending())
                .localRatio(reportResponse.getLocalRatio())
                .localGrade(reportResponse.getLocalGrade())
                .insights(reportResponse.getInsights())
                .generated(reportResponse.getGenerated())
                .build();
    }
}
