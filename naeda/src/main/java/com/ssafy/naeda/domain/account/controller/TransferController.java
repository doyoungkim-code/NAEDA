package com.ssafy.naeda.domain.account.controller;

import com.ssafy.naeda.domain.account.dto.request.TransferRequest;
import com.ssafy.naeda.domain.account.dto.response.TransferResponse;
import com.ssafy.naeda.domain.account.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Validated
public class TransferController {

    private final TransferService transferService;

    /**
     * 이체
     * POST /api/transfers?userNo=1
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @RequestParam @Positive Long userNo,
            @Valid @RequestBody TransferRequest request
    ) {
        return ResponseEntity.ok(transferService.transfer(userNo, request));
    }
}
