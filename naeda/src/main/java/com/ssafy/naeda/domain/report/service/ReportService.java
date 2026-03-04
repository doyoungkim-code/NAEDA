package com.ssafy.naeda.domain.report.service;

import com.ssafy.naeda.domain.report.dto.request.ReportSaveRequest;
import com.ssafy.naeda.domain.report.dto.response.ReportResponse;
import com.ssafy.naeda.domain.report.entity.ConsumptionReport;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import com.ssafy.naeda.domain.report.repository.ConsumptionReportRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ConsumptionReportRepository consumptionReportRepository;

    // AI 서버에서 생성한 리포트 저장
    @Transactional
    public ReportResponse saveReport(ReportSaveRequest request) {
        ConsumptionReport report = ConsumptionReport.builder()
                .userNo(request.getUserNo())
                .periodType(request.getPeriodType())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .categoryBreakdown(request.getCategoryBreakdown())
                .totalSpending(request.getTotalSpending())
                .localSpending(request.getLocalSpending())
                .localRatio(request.getLocalRatio())
                .localGrade(request.getLocalGrade())
                .insights(request.getInsights())
                .build();

        ConsumptionReport saved = consumptionReportRepository.save(report);
        return ReportResponse.from(saved);
    }

    // 최신 리포트 조회
    public ReportResponse getLatestReport(Long userNo, PeriodType periodType) {
        return consumptionReportRepository
                .findByUserNoAndPeriodTypeOrderByGenerated(userNo, periodType)
                .stream()
                .map(ReportResponse::from)
                .sorted(Comparator.comparing(ReportResponse::getGenerated).reversed())
                .findFirst()
                .orElseThrow(() -> new NotFoundException("리포트가 존재하지 않습니다."));
    }

    // 리포트 히스토리 조회
    public List<ReportResponse> getReportHistory(Long userNo, PeriodType periodType) {
        return consumptionReportRepository
                .findByUserNoAndPeriodTypeOrderByPeriodStart(userNo, periodType)
                .stream()
                .map(ReportResponse::from)
                .sorted(Comparator.comparing(ReportResponse::getPeriodStart).reversed())
                .toList();
    }
}
