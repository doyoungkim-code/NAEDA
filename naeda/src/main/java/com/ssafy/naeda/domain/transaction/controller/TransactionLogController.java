package com.ssafy.naeda.domain.transaction.controller;

import com.ssafy.naeda.domain.transaction.dto.response.TransactionLogResponse;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionLogController {

    private final TransactionLogService transactionLogService;

    /**
     * * 전체 거래내역 조회
     * GET /api/transactions?userNo=1&accountId=10
     */
    @GetMapping
    public ResponseEntity<List<TransactionLogResponse>> getTransactions(
            @RequestParam Long userNo,
            @RequestParam Long accountId,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(transactionLogService.getTransactions(userNo, accountId).stream().limit(size).toList());
    }

    /**
     * 기간 필터 거래내역 조회
     * GET /api/transactions/period?userNo=1&accountId=10&from=2026-01-01T00:00:00&to=2026-03-01T00:00:00
     */
    @GetMapping("/period")
    public ResponseEntity<List<TransactionLogResponse>> getTransactionsByPeriod(
            @RequestParam Long userNo,
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(
                transactionLogService.getTransactionsByPeriod(userNo, accountId, from, to).stream().limit(size).toList()
        );
    }

    /**
     * 거래유형 필터 거래내역 조회
     * GET /api/transactions/type?userNo=1&accountId=10&transactionType=DEPOSIT
     */
    @GetMapping("/type")
    public ResponseEntity<List<TransactionLogResponse>> getTransactionsByType(
            @RequestParam Long userNo,
            @RequestParam Long accountId,
            @RequestParam TransactionType transactionType,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(
                transactionLogService.getTransactionsByType(userNo, accountId, transactionType).stream().limit(size).toList()
        );
    }
}
