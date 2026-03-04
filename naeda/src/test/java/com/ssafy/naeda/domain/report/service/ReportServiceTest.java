package com.ssafy.naeda.domain.report.service;

import com.ssafy.naeda.domain.report.dto.request.ReportSaveRequest;
import com.ssafy.naeda.domain.report.dto.response.ReportResponse;
import com.ssafy.naeda.domain.report.entity.ConsumptionReport;
import com.ssafy.naeda.domain.report.entity.LocalGrade;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import com.ssafy.naeda.domain.report.repository.ConsumptionReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ConsumptionReportRepository consumptionReportRepository;

    private ConsumptionReport createReport(PeriodType periodType,
                                           LocalDate start, LocalDate end,
                                           Long totalSpending, LocalDateTime generated) {
        return ConsumptionReport.builder()
                .userNo(1L)
                .periodType(periodType)
                .periodStart(start)
                .periodEnd(end)
                .categoryBreakdown(Map.of("식비", 300000L, "카페", 80000L))
                .totalSpending(totalSpending)
                .localSpending(200000L)
                .localRatio(0.65f)
                .localGrade(LocalGrade.B)
                .insights(List.of("카페 지출이 전월 대비 15% 증가했습니다"))
                .generated(generated)
                .build();
    }

    @Test
    @DisplayName("AI 서버에서 생성한 리포트 저장")
    void saveReport() {
        ReportSaveRequest request = ReportSaveRequest.builder()
                .userNo(1L)
                .periodType(PeriodType.MONTHLY)
                .periodStart(LocalDate.of(2026, 2, 1))
                .periodEnd(LocalDate.of(2026, 2, 28))
                .categoryBreakdown(Map.of("식비", 300000L, "카페", 80000L))
                .totalSpending(380000L)
                .localSpending(200000L)
                .localRatio(0.65f)
                .localGrade(LocalGrade.B)
                .insights(List.of("카페 지출이 전월 대비 15% 증가했습니다"))
                .build();

        ConsumptionReport saved = createReport(PeriodType.MONTHLY,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                380000L, LocalDateTime.of(2026, 3, 1, 10, 0));

        given(consumptionReportRepository.save(any(ConsumptionReport.class))).willReturn(saved);

        ReportResponse response = reportService.saveReport(request);

        assertThat(response.getPeriodType()).isEqualTo(PeriodType.MONTHLY);
        assertThat(response.getTotalSpending()).isEqualTo(380000L);
        assertThat(response.getLocalGrade()).isEqualTo(LocalGrade.B);
        assertThat(response.getCategoryBreakdown()).containsEntry("식비", 300000L);
    }

    @Test
    @DisplayName("최신 리포트 조회 - 내림차순으로 가장 최신 반환")
    void getLatestReport() {
        ConsumptionReport older = createReport(PeriodType.MONTHLY,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                400000L, LocalDateTime.of(2026, 2, 1, 10, 0));
        ConsumptionReport newer = createReport(PeriodType.MONTHLY,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                500000L, LocalDateTime.of(2026, 3, 1, 10, 0));

        given(consumptionReportRepository.findByUserNoAndPeriodTypeOrderByGenerated(1L, PeriodType.MONTHLY))
                .willReturn(List.of(older, newer));

        ReportResponse response = reportService.getLatestReport(1L, PeriodType.MONTHLY);

        assertThat(response.getTotalSpending()).isEqualTo(500000L);
        assertThat(response.getPeriodType()).isEqualTo(PeriodType.MONTHLY);
    }

    @Test
    @DisplayName("최신 리포트 조회 실패 - 리포트 없음")
    void getLatestReportNotFound() {
        given(consumptionReportRepository.findByUserNoAndPeriodTypeOrderByGenerated(999L, PeriodType.MONTHLY))
                .willReturn(List.of());

        assertThatThrownBy(() -> reportService.getLatestReport(999L, PeriodType.MONTHLY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리포트가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("리포트 히스토리 조회 - 내림차순 정렬")
    void getReportHistory() {
        ConsumptionReport jan = createReport(PeriodType.MONTHLY,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                400000L, LocalDateTime.of(2026, 2, 1, 10, 0));
        ConsumptionReport feb = createReport(PeriodType.MONTHLY,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                500000L, LocalDateTime.of(2026, 3, 1, 10, 0));

        given(consumptionReportRepository.findByUserNoAndPeriodTypeOrderByPeriodStart(1L, PeriodType.MONTHLY))
                .willReturn(List.of(jan, feb));

        List<ReportResponse> result = reportService.getReportHistory(1L, PeriodType.MONTHLY);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTotalSpending()).isEqualTo(500000L);
        assertThat(result.get(1).getTotalSpending()).isEqualTo(400000L);
    }

    @Test
    @DisplayName("리포트 히스토리 조회 - 결과 없음")
    void getReportHistoryEmpty() {
        given(consumptionReportRepository.findByUserNoAndPeriodTypeOrderByPeriodStart(999L, PeriodType.WEEKLY))
                .willReturn(List.of());

        List<ReportResponse> result = reportService.getReportHistory(999L, PeriodType.WEEKLY);

        assertThat(result).isEmpty();
    }
}
