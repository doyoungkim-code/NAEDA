//package com.ssafy.naeda.domain.payment.controller;
//
//import com.ssafy.naeda.domain.payment.dto.request.PaymentRequest;
//import com.ssafy.naeda.domain.payment.dto.response.PaymentResponse;
//import com.ssafy.naeda.domain.payment.service.PaymentService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/payments")
//@RequiredArgsConstructor
//@Tag(name = "Payment", description = "결제 API")
//public class PaymentController {
//
//    private final PaymentService paymentService;
//
//    /**
//     * POST /api/payments
//     * 페이스페이 결제 실행.
//     *
//     * multipart/form-data:
//     *   - request (JSON): userNo, storeId, paymentMethodId, amount
//     *   - faceImage (file): 얼굴 이미지
//     */
//    @Operation(summary = "페이스페이 결제", description = "얼굴 인증 후 SSAFY 카드 결제를 실행합니다.")
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<PaymentResponse> pay(
//            @RequestPart("request") @Valid PaymentRequest request,
//            @RequestPart("faceImage") MultipartFile faceImage
//    ) {
//        return ResponseEntity.ok(paymentService.pay(request, faceImage));
//    }
//
//    /**
//     * GET /api/payments?userNo=1
//     * GET /api/payments?userNo=1&from=2025-01-01T00:00:00&to=2025-12-31T23:59:59
//     * 결제 내역 목록 조회 (기간 필터 선택).
//     */
//    @Operation(summary = "결제 내역 목록 조회", description = "사용자의 결제 내역을 조회합니다. from/to 파라미터로 기간 필터링 가능합니다.")
//    @GetMapping
//    public ResponseEntity<List<PaymentResponse>> getPayments(
//            @RequestParam Long userNo,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
//            @RequestParam(defaultValue = "100") int size
//    ) {
//        return ResponseEntity.ok(paymentService.getPayments(userNo, from, to).stream().limit(size).toList());
//    }
//
//    /**
//     * GET /api/payments/{paymentId}
//     * 결제 단건 조회.
//     */
//    @Operation(summary = "결제 단건 조회", description = "결제 ID로 단건 결제 내역을 조회합니다.")
//    @GetMapping("/{paymentId}")
//    public ResponseEntity<PaymentResponse> getPayment(
//            @RequestParam Long userNo,
//            @PathVariable Long paymentId
//    ) {
//        return ResponseEntity.ok(paymentService.getPayment(userNo, paymentId));
//    }
//}
