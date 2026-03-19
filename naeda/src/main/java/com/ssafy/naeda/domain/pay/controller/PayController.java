package com.ssafy.naeda.domain.pay.controller;

import com.ssafy.naeda.domain.pay.dto.request.PayCardRequest;
import com.ssafy.naeda.domain.pay.dto.response.PayTransactionResponse;
import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import com.ssafy.naeda.domain.pay.service.PayFacadeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Tag(name = "[Pay] 카드 결제", description = "FacePay 카드 결제 API (분산락 + 멱등성 + Kafka)")
public class PayController {

    private final PayFacadeService payFacadeService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "카드 FacePay 결제",
            description = "얼굴 인증 -> RBA 평가 -> PIN 2차인증(필요시) -> SSAFY 카드 결제 -> Kafka 이벤트 발행. "
                    + "idempotencyKey로 중복 결제 방지, 분산락으로 동시 결제 차단.")
    public ResponseEntity<PayTransactionResponse> pay(
            @Parameter(description = "사용자 번호", required = true)
            @RequestHeader("X-User-No") Long userNo,
            @Parameter(description = "결제 요청 JSON (storeId, paymentMethodId, amount, idempotencyKey, pin)")
            @RequestPart("request") String requestJson,
            @Parameter(description = "얼굴 이미지 파일")
            @RequestPart("faceImage") MultipartFile faceImage) throws Exception {

        PayCardRequest request = objectMapper.readValue(requestJson, PayCardRequest.class);

        PayTransaction tx = payFacadeService.processCardPayment(
                userNo, request.getStoreId(), request.getPaymentMethodId(),
                request.getAmount(), request.getIdempotencyKey(), faceImage,
                request.getPin()
        );
        return ResponseEntity.ok(PayTransactionResponse.from(tx));
    }

      @GetMapping
      @Operation(summary = "결제 내역 목록 조회", description = "해당 사용자의 전체 결제 내역을 최신순으로 조회합니다.")
      public ResponseEntity<List<PayTransactionResponse>> getPayments(
              @Parameter(description = "사용자 번호", required = true)
              @RequestHeader("X-User-No") Long userNo,
              @Parameter(description = "조회 시작 일시 (ISO 8601)")
              @RequestParam(required = false)LocalDateTime from,
              @Parameter(description = "조회 종료 일시 (ISO 8601)")
              @RequestParam(required = false) LocalDateTime to
              ){
        List<PayTransactionResponse> responses = payFacadeService.getPayments(userNo,from,to)
                .stream()
                .map(PayTransactionResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
      }

      @GetMapping("/{id}")
      @Operation(summary = "결제 상세 조회", description = "결제 ID로 단건 상세 조회합니다.")
      public ResponseEntity<PayTransactionResponse> getPayment(
              @Parameter(description = "사용자 번호", required = true)
              @RequestHeader("X-User-No") Long userNo,
              @Parameter(description = "결제 트랜잭션 ID") @PathVariable Long id
      ){
            PayTransaction tx = payFacadeService.getPayment(userNo, id);
            return ResponseEntity.ok(PayTransactionResponse.from(tx));
      }

//    @GetMapping
//    @Operation(summary = "결제 내역 목록 조회", description = "해당 사용자의 전체 결제 내역을 최신순으로 조회합니다.")
//    public ResponseEntity<List<PayTransactionResponse>> getPayments(
//            @Parameter(description = "사용자 번호", required = true)
//            @RequestHeader("X-User-No") Long userNo) {
//
//        List<PayTransactionResponse> responses = payFacadeService.getPayments(userNo)
//                .stream()
//                .map(PayTransactionResponse::from)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(responses);
//    }

//    @GetMapping("/{id}")
//    @Operation(summary = "결제 상세 조회", description = "결제 ID로 단건 상세 조회합니다.")
//    public ResponseEntity<PayTransactionResponse> getPayment(
//            @Parameter(description = "결제 트랜잭션 ID") @PathVariable Long id) {
//
//        PayTransaction tx = payFacadeService.getPayment(id);
//        return ResponseEntity.ok(PayTransactionResponse.from(tx));
//    }
}
