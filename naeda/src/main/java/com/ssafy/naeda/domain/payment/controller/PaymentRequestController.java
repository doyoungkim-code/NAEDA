package com.ssafy.naeda.domain.payment.controller;

import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.payment.dto.request.CreatePaymentRequestRequest;
import com.ssafy.naeda.domain.payment.dto.response.PaymentRequestResponse;
import com.ssafy.naeda.domain.payment.dto.response.ProcessPaymentResponse;
import com.ssafy.naeda.domain.payment.service.PaymentProcessService;
import com.ssafy.naeda.domain.payment.service.PaymentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/payment-requests")
@RequiredArgsConstructor
@Tag(name = "Payment Request", description = "결제 요청 API (단말기 폴링 + 사용자 결제 처리)")
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final PaymentProcessService paymentProcessService;

    @Operation(summary = "결제 요청 생성", description = "단말기에서 결제 요청을 생성합니다. Redis에 PENDING 상태로 저장됩니다.")
    @PostMapping
    public ResponseEntity<PaymentRequestResponse> createRequest(
            @RequestBody @Valid CreatePaymentRequestRequest request
    ) {
        PaymentRequestData data = paymentRequestService.createPaymentRequest(
                request.getStoreId(), request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentRequestResponse.from(data));
    }

    @Operation(summary = "결제 요청 상태 조회 (폴링)", description = "requestId로 결제 요청의 현재 상태를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<PaymentRequestResponse> getRequest(
            @PathVariable String requestId
    ) {
        PaymentRequestData data = paymentRequestService.getPaymentRequest(requestId);
        return ResponseEntity.ok(PaymentRequestResponse.from(data));
    }

    @Operation(summary = "결제 요청 처리", description = "사용자 앱에서 얼굴 인증 후 결제를 처리합니다.")
    @PostMapping(value = "/{requestId}/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessPaymentResponse> processPayment(
            @PathVariable String requestId,
            @RequestPart("faceImage") MultipartFile faceImage
    ) {
        ProcessPaymentResponse response = paymentProcessService.processPayment(requestId, faceImage);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "매장별 결제 요청 목록 조회", description = "매장 ID로 현재 활성 결제 요청 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<PaymentRequestResponse>> getRequestsByStore(
            @RequestParam Long storeId
    ) {
        List<PaymentRequestData> dataList = paymentRequestService.getPaymentRequestsByStore(storeId);
        List<PaymentRequestResponse> responses = dataList.stream()
                .map(PaymentRequestResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
