package com.ssafy.naeda.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.payment.dto.response.ProcessPaymentResponse;
import com.ssafy.naeda.domain.payment.entity.PaymentRequestStatus;
import com.ssafy.naeda.domain.payment.service.PaymentProcessService;
import com.ssafy.naeda.domain.payment.service.PaymentRequestService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentRequestService paymentRequestService;

    @MockitoBean
    private PaymentProcessService paymentProcessService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final Long STORE_ID = 100L;
    private static final Long AMOUNT = 15000L;

    // ── POST /api/payment-requests ───────────────────────────────────────

    @Test
    @DisplayName("결제 요청 생성 - 201 Created와 requestId를 반환한다")
    void createRequest_returns201() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(paymentRequestService.createPaymentRequest(STORE_ID, AMOUNT)).willReturn(data);

        mockMvc.perform(post("/api/payment-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("storeId", STORE_ID, "amount", AMOUNT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value("test-uuid"))
                .andExpect(jsonPath("$.storeId").value(STORE_ID))
                .andExpect(jsonPath("$.amount").value(AMOUNT))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("결제 요청 생성 - 존재하지 않는 매장이면 404를 반환한다")
    void createRequest_storeNotFound_returns404() throws Exception {
        given(paymentRequestService.createPaymentRequest(anyLong(), anyLong()))
                .willThrow(new NotFoundException("존재하지 않는 매장입니다."));

        mockMvc.perform(post("/api/payment-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("storeId", 999, "amount", 15000))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 매장입니다."));
    }

    @Test
    @DisplayName("결제 요청 생성 - 페이스페이 미지원 매장이면 400을 반환한다")
    void createRequest_facePayDisabled_returns400() throws Exception {
        given(paymentRequestService.createPaymentRequest(anyLong(), anyLong()))
                .willThrow(new BadRequestException("해당 매장은 페이스페이를 지원하지 않습니다."));

        mockMvc.perform(post("/api/payment-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("storeId", 100, "amount", 15000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("결제 요청 생성 - 금액이 0 이하이면 400을 반환한다")
    void createRequest_invalidAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/payment-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("storeId", 100, "amount", -1))))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/payment-requests/{requestId} ────────────────────────────

    @Test
    @DisplayName("결제 요청 폴링 - 200 OK와 요청 상태를 반환한다")
    void getRequest_returns200() throws Exception {
        PaymentRequestData data = PaymentRequestData.builder()
                .requestId("test-uuid")
                .storeId(STORE_ID)
                .amount(AMOUNT)
                .status(PaymentRequestStatus.PROCESSING)
                .userNo(10L)
                .nextAction("PASS")
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        given(paymentRequestService.getPaymentRequest("test-uuid")).willReturn(data);

        mockMvc.perform(get("/api/payment-requests/test-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("test-uuid"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.userNo").value(10))
                .andExpect(jsonPath("$.nextAction").value("PASS"));
    }

    @Test
    @DisplayName("결제 요청 폴링 - 만료된 요청이면 404를 반환한다")
    void getRequest_expired_returns404() throws Exception {
        given(paymentRequestService.getPaymentRequest("expired-uuid"))
                .willThrow(new NotFoundException("결제 요청이 만료되었거나 존재하지 않습니다."));

        mockMvc.perform(get("/api/payment-requests/expired-uuid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ── GET /api/payment-requests?storeId= ───────────────────────────────

    @Test
    @DisplayName("매장별 목록 조회 - 200 OK와 요청 목록을 반환한다")
    void getRequestsByStore_returns200() throws Exception {
        PaymentRequestData data1 = PaymentRequestData.builder()
                .requestId("uuid-1").storeId(STORE_ID).amount(10000L)
                .status(PaymentRequestStatus.PENDING)
                .createdAt(System.currentTimeMillis()).updatedAt(System.currentTimeMillis())
                .build();
        PaymentRequestData data2 = PaymentRequestData.builder()
                .requestId("uuid-2").storeId(STORE_ID).amount(20000L)
                .status(PaymentRequestStatus.PROCESSING)
                .createdAt(System.currentTimeMillis()).updatedAt(System.currentTimeMillis())
                .build();

        given(paymentRequestService.getPaymentRequestsByStore(STORE_ID))
                .willReturn(List.of(data1, data2));

        mockMvc.perform(get("/api/payment-requests").param("storeId", STORE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].requestId").value("uuid-1"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].requestId").value("uuid-2"))
                .andExpect(jsonPath("$[1].status").value("PROCESSING"));
    }

    @Test
    @DisplayName("매장별 목록 조회 - 활성 요청 없으면 빈 배열을 반환한다")
    void getRequestsByStore_empty_returns200() throws Exception {
        given(paymentRequestService.getPaymentRequestsByStore(STORE_ID))
                .willReturn(List.of());

        mockMvc.perform(get("/api/payment-requests").param("storeId", STORE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("매장별 목록 조회 - storeId 누락 시 400을 반환한다")
    void getRequestsByStore_missingStoreId_returns400() throws Exception {
        mockMvc.perform(get("/api/payment-requests"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/payment-requests/{requestId}/process ────────────────────

    @Test
    @DisplayName("결제 처리 - 성공 시 200과 SUCCESS를 반환한다")
    void processPayment_returns200() throws Exception {
        ProcessPaymentResponse response = ProcessPaymentResponse.builder()
                .requestId("test-uuid").paymentId(50L).status("SUCCESS")
                .nextAction("PASS").storeId(STORE_ID).amount(AMOUNT)
                .earnedPoints(750).ssafyTransactionId("TXN-001")
                .similarity(0.95).build();

        given(paymentProcessService.processPayment(eq("test-uuid"), any(), any()))
                .willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"userNo\":10}".getBytes());
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes());

        mockMvc.perform(multipart("/api/payment-requests/test-uuid/process")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentId").value(50))
                .andExpect(jsonPath("$.earnedPoints").value(750))
                .andExpect(jsonPath("$.nextAction").value("PASS"));
    }

    @Test
    @DisplayName("결제 처리 - 얼굴 차단 시 200과 BLOCKED를 반환한다")
    void processPayment_blocked_returns200() throws Exception {
        ProcessPaymentResponse response = ProcessPaymentResponse.builder()
                .requestId("test-uuid").status("BLOCKED")
                .nextAction("BLOCK").storeId(STORE_ID).amount(AMOUNT)
                .similarity(0.3).build();

        given(paymentProcessService.processPayment(eq("test-uuid"), any(), any()))
                .willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"userNo\":10}".getBytes());
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes());

        mockMvc.perform(multipart("/api/payment-requests/test-uuid/process")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.nextAction").value("BLOCK"));
    }

    @Test
    @DisplayName("결제 처리 - 요청 만료 시 404를 반환한다")
    void processPayment_expired_returns404() throws Exception {
        given(paymentProcessService.processPayment(eq("expired-uuid"), any(), any()))
                .willThrow(new NotFoundException("결제 요청이 만료되었거나 존재하지 않습니다."));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"userNo\":10}".getBytes());
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes());

        mockMvc.perform(multipart("/api/payment-requests/expired-uuid/process")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("결제 처리 - 이미 처리 중인 요청이면 400을 반환한다")
    void processPayment_alreadyProcessing_returns400() throws Exception {
        given(paymentProcessService.processPayment(eq("processing-uuid"), any(), any()))
                .willThrow(new BadRequestException("이미 처리 중이거나 완료된 결제 요청입니다."));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"userNo\":10}".getBytes());
        MockMultipartFile faceImage = new MockMultipartFile(
                "faceImage", "face.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes());

        mockMvc.perform(multipart("/api/payment-requests/processing-uuid/process")
                        .file(requestPart)
                        .file(faceImage))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("결제 처리 - 얼굴 이미지 누락 시 400을 반환한다")
    void processPayment_missingFaceImage_returns400() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"userNo\":10}".getBytes());

        mockMvc.perform(multipart("/api/payment-requests/test-uuid/process")
                        .file(requestPart))
                .andExpect(status().isBadRequest());
    }
}
