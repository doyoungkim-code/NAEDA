package com.ssafy.naeda.domain.point.controller;

import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.dto.request.PointUseRequest;
import com.ssafy.naeda.domain.point.dto.response.PointHistoryResponse;
import com.ssafy.naeda.domain.point.dto.response.PointWalletResponse;
import com.ssafy.naeda.domain.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/wallet/{userNo}")
    public ResponseEntity<PointWalletResponse> createWallet(@PathVariable Long userNo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pointService.createWallet(userNo));
    }

    @GetMapping("/wallet/{userNo}")
    public ResponseEntity<PointWalletResponse> getWallet(@PathVariable Long userNo) {
        return ResponseEntity.ok(pointService.getWallet(userNo));
    }

    @PostMapping("/wallet/{userNo}/earn")
    public ResponseEntity<PointWalletResponse> earnPoints(
            @PathVariable Long userNo,
            @Valid @RequestBody PointEarnRequest request) {
        return ResponseEntity.ok(pointService.earnPoints(userNo, request));
    }

    @PostMapping("/wallet/{userNo}/use")
    public ResponseEntity<PointWalletResponse> usePoints(
            @PathVariable Long userNo,
            @Valid @RequestBody PointUseRequest request) {
        return ResponseEntity.ok(pointService.usePoints(userNo, request));
    }

    @GetMapping("/wallet/{userNo}/histories")
    public ResponseEntity<List<PointHistoryResponse>> getHistories(@PathVariable Long userNo) {
        return ResponseEntity.ok(pointService.getHistories(userNo));
    }
}
