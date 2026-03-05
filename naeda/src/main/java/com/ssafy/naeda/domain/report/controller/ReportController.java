package com.ssafy.naeda.domain.report.controller;

import com.ssafy.naeda.domain.report.dto.response.ReportResponse;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import com.ssafy.naeda.domain.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "소비 리포트", description = "소비 리포트 조회 API")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/latest")
    @Operation(summary = "최신 소비 리포트 조회", description = "사용자의 가장 최근 소비 리포트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<ReportResponse> getLatestReport(
            @Parameter(description = "사용자 번호", example = "1", required = true)
            @RequestParam Long userNo,
            @Parameter(description = "리포트 기간 유형", example = "WEEKLY", required = true)
            @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportService.getLatestReport(userNo, periodType));
    }

    @GetMapping
    @Operation(summary = "소비 리포트 히스토리 조회", description = "사용자의 소비 리포트 목록을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<ReportResponse>> getReportHistory(
            @Parameter(description = "사용자 번호", example = "1", required = true)
            @RequestParam Long userNo,
            @Parameter(description = "리포트 기간 유형", example = "MONTHLY", required = true)
            @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(reportService.getReportHistory(userNo, periodType));
    }
}
