package com.ssafy.naeda.domain.point.controller;

import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.dto.request.PointUseRequest;
import com.ssafy.naeda.domain.point.dto.response.PointHistoryResponse;
import com.ssafy.naeda.domain.point.dto.response.PointWalletResponse;
import com.ssafy.naeda.domain.point.service.PointService;
import com.ssafy.naeda.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Tag(name = "포인트", description = "포인트 지갑 생성, 적립, 사용, 조회 API")
public class PointController {

    private final PointService pointService;

    @PostMapping("/wallet/{userNo}")
    @Operation(summary = "포인트 지갑 생성", description = "사용자 번호 기준으로 포인트 지갑을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PointWalletResponse> createWallet(@PathVariable Long userNo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pointService.createWallet(userNo));
    }

    @GetMapping("/wallet/{userNo}")
    @Operation(summary = "포인트 지갑 조회", description = "사용자 포인트 잔액 및 누적 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<PointWalletResponse> getWallet(@PathVariable Long userNo) {
        return ResponseEntity.ok(pointService.getWallet(userNo));
    }

    @PostMapping("/wallet/{userNo}/earn")
    @Operation(summary = "포인트 적립", description = "사용자 지갑에 포인트를 적립합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "적립 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PointWalletResponse> earnPoints(
            @Parameter(description = "사용자 번호", example = "1", required = true)
            @PathVariable Long userNo,
            @Valid @RequestBody PointEarnRequest request) {
        return ResponseEntity.ok(pointService.earnPoints(userNo, request));
    }

    @PostMapping("/wallet/{userNo}/use")
    @Operation(summary = "포인트 사용", description = "사용자 지갑에서 포인트를 차감합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PointWalletResponse> usePoints(
            @Parameter(description = "사용자 번호", example = "1", required = true)
            @PathVariable Long userNo,
            @Valid @RequestBody PointUseRequest request) {
        return ResponseEntity.ok(pointService.usePoints(userNo, request));
    }

    @GetMapping("/wallet/{userNo}/histories")
    @Operation(summary = "포인트 이력 조회", description = "사용자 포인트 적립/사용 이력을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<PointHistoryResponse>> getHistories(@PathVariable Long userNo) {
        return ResponseEntity.ok(pointService.getHistories(userNo));
    }
}
