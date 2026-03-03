package com.ssafy.naeda.domain.report.repository;

import com.ssafy.naeda.domain.report.entity.ConsumptionReport;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsumptionReportRepository extends JpaRepository<ConsumptionReport, Long> {

    Optional<ConsumptionReport> findTopByUserNoAndPeriodTypeOrderByGenerated(Long userNo, PeriodType periodType);

    List<ConsumptionReport> findByUserNoAndPeriodTypeOrderByPeriodStart(Long userNo, PeriodType periodType);
}