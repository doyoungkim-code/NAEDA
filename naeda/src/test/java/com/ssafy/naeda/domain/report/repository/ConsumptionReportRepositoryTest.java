package com.ssafy.naeda.domain.report.repository;

import com.ssafy.naeda.domain.report.entity.ConsumptionReport;
import com.ssafy.naeda.domain.report.entity.LocalGrade;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConsumptionReportRepositoryTest {

    @Autowired
    private ConsumptionReportRepository consumptionReportRepository;

    @BeforeEach
    void setUp() {
        consumptionReportRepository.deleteAll();
    }

    @Test
    @DisplayName("소비 리포트 저장 및 조회")
    void saveAndFind() {
        ConsumptionReport report = consumptionReportRepository.save(
                ConsumptionReport.builder()
                        .userNo(1L)
                        .periodType(PeriodType.WEEKLY)
                        .periodStart(LocalDate.of(2026, 2, 24))
                        .periodEnd(LocalDate.of(2026, 3, 2))
                        .categoryBreakdown(Map.of("식비", 320000L, "카페", 85000L))
                        .totalSpending(405000L)
                        .localSpending(200000L)
                        .localRatio(0.49f)
                        .localGrade(LocalGrade.B)
                        .insights(List.of("카페 지출이 전주 대비 20% 증가했어요"))
                        .build()
        );

        Optional<ConsumptionReport> found = consumptionReportRepository.findById(report.getReportId());

        assertThat(found).isPresent();
        ConsumptionReport result = found.get();
        assertThat(result.getUserNo()).isEqualTo(1L);
        assertThat(result.getPeriodType()).isEqualTo(PeriodType.WEEKLY);
        assertThat(result.getTotalSpending()).isEqualTo(405000L);
        assertThat(result.getCategoryBreakdown()).containsEntry("식비", 320000L);
        assertThat(result.getInsights()).contains("카페 지출이 전주 대비 20% 증가했어요");
        assertThat(result.getLocalGrade()).isEqualTo(LocalGrade.B);
    }

    @Test
    @DisplayName("userNo + periodType으로 generated 오름차순 리포트 목록 조회")
    void findByUserNoAndPeriodTypeOrderByGenerated() {
        consumptionReportRepository.save(
                ConsumptionReport.builder()
                        .userNo(1L)
                        .periodType(PeriodType.MONTHLY)
                        .periodStart(LocalDate.of(2026, 1, 1))
                        .periodEnd(LocalDate.of(2026, 1, 31))
                        .totalSpending(500000L)
                        .build()
        );
        consumptionReportRepository.flush();

        consumptionReportRepository.save(
                ConsumptionReport.builder()
                        .userNo(1L)
                        .periodType(PeriodType.MONTHLY)
                        .periodStart(LocalDate.of(2026, 2, 1))
                        .periodEnd(LocalDate.of(2026, 2, 28))
                        .totalSpending(600000L)
                        .build()
        );

        List<ConsumptionReport> result = consumptionReportRepository
                .findByUserNoAndPeriodTypeOrderByGenerated(1L, PeriodType.MONTHLY);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTotalSpending()).isEqualTo(500000L);
        assertThat(result.get(1).getTotalSpending()).isEqualTo(600000L);
    }

    @Test
    @DisplayName("userNo + periodType으로 리포트 목록 조회 - periodStart 오름차순")
    void findByUserNoAndPeriodType() {
        consumptionReportRepository.save(
                ConsumptionReport.builder()
                        .userNo(1L)
                        .periodType(PeriodType.WEEKLY)
                        .periodStart(LocalDate.of(2026, 2, 24))
                        .periodEnd(LocalDate.of(2026, 3, 2))
                        .totalSpending(300000L)
                        .build()
        );
        consumptionReportRepository.save(
                ConsumptionReport.builder()
                        .userNo(1L)
                        .periodType(PeriodType.WEEKLY)
                        .periodStart(LocalDate.of(2026, 2, 17))
                        .periodEnd(LocalDate.of(2026, 2, 23))
                        .totalSpending(250000L)
                        .build()
        );
        consumptionReportRepository.save(
                ConsumptionReport.builder()
                        .userNo(1L)
                        .periodType(PeriodType.MONTHLY)
                        .periodStart(LocalDate.of(2026, 2, 1))
                        .periodEnd(LocalDate.of(2026, 2, 28))
                        .totalSpending(700000L)
                        .build()
        );

        List<ConsumptionReport> weeklyReports = consumptionReportRepository
                .findByUserNoAndPeriodTypeOrderByPeriodStart(1L, PeriodType.WEEKLY);

        assertThat(weeklyReports).hasSize(2);
        assertThat(weeklyReports.get(0).getPeriodStart()).isBefore(weeklyReports.get(1).getPeriodStart());
    }

    @Test
    @DisplayName("다른 userNo의 리포트는 조회되지 않음")
    void findByDifferentUserNo() {
        consumptionReportRepository.save(
                ConsumptionReport.builder()
                        .userNo(1L)
                        .periodType(PeriodType.WEEKLY)
                        .periodStart(LocalDate.of(2026, 2, 24))
                        .periodEnd(LocalDate.of(2026, 3, 2))
                        .totalSpending(300000L)
                        .build()
        );

        List<ConsumptionReport> result = consumptionReportRepository
                .findByUserNoAndPeriodTypeOrderByPeriodStart(2L, PeriodType.WEEKLY);

        assertThat(result).isEmpty();
    }
}
