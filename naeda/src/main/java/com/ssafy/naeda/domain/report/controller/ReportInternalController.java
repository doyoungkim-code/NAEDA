package com.ssafy.naeda.domain.report.controller;

import com.ssafy.naeda.domain.report.dto.request.ReportSaveRequest;
import com.ssafy.naeda.domain.report.dto.response.ReportResponse;
import com.ssafy.naeda.domain.report.service.ReportService;
import com.ssafy.naeda.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/reports")
@Tag(name = "소비 리포트 (내부)", description = "AI 서버에서 호출하는 소비 리포트 저장 API")
public class ReportInternalController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "소비 리포트 저장", description = "AI 서버에서 생성한 소비 리포트를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReportResponse> saveReport(@RequestBody @Valid ReportSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.saveReport(request));
    }
}
