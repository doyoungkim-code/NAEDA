package com.ssafy.naeda.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.payment.dto.response.PaymentResponse;
import com.ssafy.naeda.domain.payment.service.PaymentService;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── POST /api/payments ───────────────────────────────────────────────

    @Test
    @DisplayName("결제 성공 (PASS) - 200 OK와 결제 결과를 반환한다")
    void pay_pass_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId(1L)
                .userNo(10L)
                .storeId(100L)
                .amount(15000L)
                .authMethod("FACE_PAY")
                .authLevel("FACE_ONLY")
                .status("SUCCESS")
                .earnedPoints(750)
                .nextAction("PASS")
                .similarity(0.92)
                .build();

        given(paymentService.pay(any(), any())).willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of("storeId", 100, "amount", 15000))
        );
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/payments")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.nextAction").value("PASS"))
                .andExpect(jsonPath("$.earnedPoints").value(750));
    }

    @Test
    @DisplayName("결제 차단 (BLOCK) - 200 OK와 BLOCKED 상태를 반환한다")
    void pay_blocked_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .status("BLOCKED")
                .nextAction("BLOCK")
                .similarity(0.45)
                .rbaReason("얼굴 불일치")
                .build();

        given(paymentService.pay(any(), any())).willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of("storeId", 100, "amount", 15000))
        );
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/payments")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.nextAction").value("BLOCK"));
    }

    @Test
    @DisplayName("2차 인증 필요 - 200 OK와 REQUIRE_SECOND_FACTOR 응답을 반환한다")
    void pay_requireSecondFactor_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId(2L)
                .userNo(10L)
                .storeId(100L)
                .amount(150000L)
                .status("FAILED")
                .nextAction("REQUIRE_SECOND_FACTOR")
                .similarity(0.85)
                .rbaReason("고액 결제")
                .build();

        given(paymentService.pay(any(), any())).willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of("storeId", 100, "amount", 150000))
        );
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/payments")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextAction").value("REQUIRE_SECOND_FACTOR"))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @DisplayName("결제 실패 - 페이스페이 미지원 매장이면 400을 반환한다")
    void pay_facePayNotSupported_returns400() throws Exception {
        given(paymentService.pay(any(), any()))
                .willThrow(new BadRequestException("해당 매장은 페이스페이를 지원하지 않습니다."));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of("storeId", 999, "amount", 15000))
        );
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/payments")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("결제 실패 - 존재하지 않는 매장이면 404를 반환한다")
    void pay_storeNotFound_returns404() throws Exception {
        given(paymentService.pay(any(), any()))
                .willThrow(new NotFoundException("존재하지 않는 매장입니다."));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of("storeId", 999, "amount", 15000))
        );
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/payments")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 매장입니다."));
    }

    // ── GET /api/payments ────────────────────────────────────────────────

    @Test
    @DisplayName("결제 목록 조회 - userNo로 목록 조회 시 200을 반환한다")
    void getPayments_returns200() throws Exception {
        given(paymentService.getPayments(1L, null, null)).willReturn(List.of(
                PaymentResponse.builder()
                        .paymentId(1L).userNo(1L).storeId(100L).amount(15000L)
                        .status("SUCCESS").authMethod("FACE_PAY").authLevel("FACE_ONLY")
                        .earnedPoints(750).paid(LocalDateTime.of(2026, 1, 1, 12, 0))
                        .build(),
                PaymentResponse.builder()
                        .paymentId(2L).userNo(1L).storeId(200L).amount(30000L)
                        .status("SUCCESS").authMethod("FACE_PAY").authLevel("FACE_ONLY")
                        .earnedPoints(1500).paid(LocalDateTime.of(2026, 1, 2, 14, 0))
                        .build()
        ));

        mockMvc.perform(get("/api/payments").param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].paymentId").value(1))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[1].paymentId").value(2));
    }

    @Test
    @DisplayName("결제 목록 조회 - userNo 누락 시 400을 반환한다")
    void getPayments_missingUserNo_returns400() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/payments/{paymentId} ────────────────────────────────────

    @Test
    @DisplayName("결제 단건 조회 - 200 OK와 결제 정보를 반환한다")
    void getPayment_returns200() throws Exception {
        given(paymentService.getPayment(1L, 1L)).willReturn(
                PaymentResponse.builder()
                        .paymentId(1L).userNo(10L).storeId(100L).amount(15000L)
                        .status("SUCCESS").authMethod("FACE_PAY").authLevel("FACE_ONLY")
                        .earnedPoints(750)
                        .build()
        );

        mockMvc.perform(get("/api/payments/1").param("userNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.amount").value(15000))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.earnedPoints").value(750));
    }

    @Test
    @DisplayName("결제 단건 조회 - 존재하지 않는 paymentId면 404를 반환한다")
    void getPayment_notFound_returns404() throws Exception {
        given(paymentService.getPayment(1L, 999L))
                .willThrow(new NotFoundException("존재하지 않는 결제 내역입니다."));

        mockMvc.perform(get("/api/payments/999").param("userNo", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 결제 내역입니다."));
    }
}
