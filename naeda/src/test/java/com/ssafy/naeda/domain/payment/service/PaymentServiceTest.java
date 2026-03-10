package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.service.FaceService;
import com.ssafy.naeda.domain.payment.dto.request.PaymentRequest;
import com.ssafy.naeda.domain.payment.dto.response.PaymentResponse;
import com.ssafy.naeda.domain.payment.entity.MethodType;
import com.ssafy.naeda.domain.payment.entity.Payment;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import com.ssafy.naeda.domain.payment.repository.PaymentRepository;
import com.ssafy.naeda.domain.point.service.PointService;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private DebitCardRepository debitCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private FaceService faceService;
    @Mock private PointService pointService;
    @Mock private SsafyApiClient ssafyApiClient;
    @Mock private SsafyHeaderFactory ssafyHeaderFactory;

    @InjectMocks
    private PaymentService paymentService;

    private static final Long STORE_ID   = 100L;
    private static final Long USER_NO    = 10L;
    private static final String USER_ID  = "user@example.com";
    private static final String USER_KEY = "test-user-key";
    private static final Long AMOUNT     = 15000L;
    private static final Long PM_ID      = 50L;
    private static final Long CARD_ID    = 200L;

    private Store facePayStore;
    private User  stubUser;
    private PaymentMethod creditPaymentMethod;
    private CreditCard stubCreditCard;
    private MultipartFile faceImage;

    @BeforeEach
    void setUp() throws Exception {
        facePayStore = Store.builder()
                .storeId(STORE_ID).userNo(1L).storeName("테스트 매장")
                .categoryId("CG-test").roadAddress("경북 구미시 대학로 1")
                .facePayEnabled(true).build();

        stubUser = User.builder()
                .userNo(USER_NO).userId(USER_ID)
                .password("pw").username("테스터").residentNo("9901011")
                .phone("010-0000-0000").institutionCode("M220516185630")
                .userKey(USER_KEY).build();

        creditPaymentMethod = PaymentMethod.builder()
                .userNo(USER_NO).methodType(MethodType.CREDIT_CARD)
                .creditCardId(CARD_ID).build();
        setField(creditPaymentMethod, "paymentMethodId", PM_ID);

        stubCreditCard = CreditCard.builder()
                .userNo(USER_NO).cardNo("1003622654847049").cvc("713")
                .cardUniqueNo("1003-unique-abc").cardIssuerCode("1003")
                .cardIssuerName("롯데카드").cardName("디지로카 SEOUL")
                .cardExpiryDate("20290409").creditLimit(200_000L)
                .billingDate(15).accountId(1L).build();
        setField(stubCreditCard, "creditCardId", CARD_ID);

        faceImage = new MockMultipartFile("faceImage", "face.jpg", "image/jpeg", "fake".getBytes());

        // PaymentService에 @Value pointRate=0.05 주입
        setField(paymentService, "pointRate", 0.05);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f;
        try {
            f = target.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            f = target.getClass().getSuperclass().getDeclaredField(name);
        }
        f.setAccessible(true);
        f.set(target, value);
    }

    private PaymentRequest buildRequest() throws Exception {
        PaymentRequest req = new PaymentRequest();
        setField(req, "storeId", STORE_ID);
        setField(req, "amount", AMOUNT);
        return req;
    }

    private SearchResponse buildSearchResponse(float similarity, boolean blocked, String nextAction,
                                               com.ssafy.naeda.domain.rba.dto.AuthLevel authLevel,
                                               String bestUserId, String rbaReason) {
        return SearchResponse.builder()
                .similarity(similarity)
                .blocked(blocked)
                .nextAction(nextAction)
                .authLevel(authLevel)
                .bestUserId(bestUserId)
                .rbaReason(rbaReason)
                .build();
    }

    private Payment savedPayment(Long paymentId) throws Exception {
        Payment p = Payment.builder()
                .userNo(USER_NO).storeId(STORE_ID).paymentMethodId(PM_ID)
                .amount(AMOUNT).authMethod(com.ssafy.naeda.domain.payment.entity.AuthMethod.FACE_PAY)
                .authLevel(com.ssafy.naeda.domain.payment.entity.AuthLevel.FACE_ONLY)
                .build();
        setField(p, "paymentId", paymentId);
        p.updateStatus(com.ssafy.naeda.domain.payment.entity.PaymentStatus.SUCCESS);
        return p;
    }

    // ── PASS 흐름 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 성공 (PASS) - Payment가 SUCCESS 상태로 저장되고 포인트가 적립된다")
    void pay_pass_success() throws Exception {
        SearchResponse faceResult = buildSearchResponse(
                0.92f, false, "PASS",
                com.ssafy.naeda.domain.rba.dto.AuthLevel.FACE_ONLY,
                USER_ID, null);

        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(facePayStore));
        given(faceService.search(any(), anyInt(), anyLong())).willReturn(faceResult);
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(stubUser));
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(creditPaymentMethod));
        given(creditCardRepository.findById(CARD_ID)).willReturn(Optional.of(stubCreditCard));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        lenient().when(ssafyApiClient.buildBody(any(), (Object[]) any())).thenReturn(Map.of());
        Map<String, Object> rec = new HashMap<>();
        rec.put("transactionUniqueNo", "TX-001");
        given(ssafyApiClient.post(anyString(), any())).willReturn(Map.of("REC", rec));
        Payment savedP = savedPayment(1L);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedP);

        PaymentResponse result = paymentService.pay(buildRequest(), faceImage);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getNextAction()).isEqualTo("PASS");
        verify(pointService).earnPoints(any(), any());
    }

    @Test
    @DisplayName("결제 성공 - amount * 5% 포인트가 earnedPoints에 반영된다")
    void pay_points_calculated_at_5percent() throws Exception {
        SearchResponse faceResult = buildSearchResponse(
                0.92f, false, "PASS",
                com.ssafy.naeda.domain.rba.dto.AuthLevel.FACE_ONLY,
                USER_ID, null);

        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(facePayStore));
        given(faceService.search(any(), anyInt(), anyLong())).willReturn(faceResult);
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(stubUser));
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(creditPaymentMethod));
        given(creditCardRepository.findById(CARD_ID)).willReturn(Optional.of(stubCreditCard));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        lenient().when(ssafyApiClient.buildBody(any(), (Object[]) any())).thenReturn(Map.of());
        Map<String, Object> rec = new HashMap<>();
        rec.put("transactionUniqueNo", "TX-002");
        given(ssafyApiClient.post(anyString(), any())).willReturn(Map.of("REC", rec));

        // 저장 시 earnedPoints 검증용 — argCaptor 대신 willAnswer 사용
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> {
            Payment p = inv.getArgument(0);
            // earnedPoints = 15000 * 0.05 = 750
            assertThat(p.getEarnedPoints()).isEqualTo(750);
            return p;
        });

        paymentService.pay(buildRequest(), faceImage);
    }

    // ── BLOCKED 흐름 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 차단 (BLOCKED) - Payment를 저장하지 않고 BLOCKED 응답을 반환한다")
    void pay_blocked_noPaymentSaved() throws Exception {
        SearchResponse faceResult = buildSearchResponse(
                0.40f, true, "BLOCK",
                com.ssafy.naeda.domain.rba.dto.AuthLevel.BLOCKED,
                null, "얼굴 불일치");

        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(facePayStore));
        given(faceService.search(any(), anyInt(), anyLong())).willReturn(faceResult);

        PaymentResponse result = paymentService.pay(buildRequest(), faceImage);

        assertThat(result.getStatus()).isEqualTo("BLOCKED");
        assertThat(result.getNextAction()).isEqualTo("BLOCK");
        assertThat(result.getPaymentId()).isNull();
        verify(paymentRepository, never()).save(any());
        verify(ssafyApiClient, never()).post(anyString(), any());
    }

    // ── REQUIRE_SECOND_FACTOR 흐름 ───────────────────────────────────────

    @Test
    @DisplayName("2차 인증 필요 - Payment(FAILED)를 저장하고 SSAFY API를 호출하지 않는다")
    void pay_requireSecondFactor_savesFailedPayment() throws Exception {
        SearchResponse faceResult = buildSearchResponse(
                0.85f, false, "REQUIRE_SECOND_FACTOR",
                com.ssafy.naeda.domain.rba.dto.AuthLevel.FACE_PHONE,
                USER_ID, "고액 결제");

        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(facePayStore));
        given(faceService.search(any(), anyInt(), anyLong())).willReturn(faceResult);
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(stubUser));
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(creditPaymentMethod));
        given(creditCardRepository.findById(CARD_ID)).willReturn(Optional.of(stubCreditCard));
        Payment failedP = Payment.builder()
                .userNo(USER_NO).storeId(STORE_ID).paymentMethodId(PM_ID).amount(AMOUNT)
                .authMethod(com.ssafy.naeda.domain.payment.entity.AuthMethod.FACE_PAY)
                .authLevel(com.ssafy.naeda.domain.payment.entity.AuthLevel.FACE_PHONE)
                .build();
        setField(failedP, "paymentId", 2L);
        given(paymentRepository.save(any(Payment.class))).willReturn(failedP);

        PaymentResponse result = paymentService.pay(buildRequest(), faceImage);

        assertThat(result.getNextAction()).isEqualTo("REQUIRE_SECOND_FACTOR");
        verify(paymentRepository).save(any(Payment.class));
        verify(ssafyApiClient, never()).post(anyString(), any());
        verify(pointService, never()).earnPoints(any(), any());
    }

    // ── 예외 케이스 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 실패 - 존재하지 않는 매장이면 NotFoundException")
    void pay_storeNotFound() throws Exception {
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.pay(buildRequest(), faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 매장입니다.");
    }

    @Test
    @DisplayName("결제 실패 - 페이스페이 미지원 매장이면 BadRequestException")
    void pay_facePayDisabled() throws Exception {
        Store nonFacePayStore = Store.builder()
                .storeId(STORE_ID).userNo(1L).storeName("일반 매장")
                .categoryId("CG-test").roadAddress("경북 구미시 대학로 1")
                .facePayEnabled(false).build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(nonFacePayStore));

        assertThatThrownBy(() -> paymentService.pay(buildRequest(), faceImage))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("페이스페이를 지원하지 않습니다.");
    }

    @Test
    @DisplayName("결제 실패 - 페이스페이 결제 수단 미등록이면 NotFoundException")
    void pay_noFacePayMethod() throws Exception {
        SearchResponse faceResult = buildSearchResponse(
                0.92f, false, "PASS",
                com.ssafy.naeda.domain.rba.dto.AuthLevel.FACE_ONLY,
                USER_ID, null);

        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(facePayStore));
        given(faceService.search(any(), anyInt(), anyLong())).willReturn(faceResult);
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(stubUser));
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.pay(buildRequest(), faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("페이스페이 결제 수단이 등록되지 않았습니다.");
    }

    // ── getPayments ──────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 목록 조회 - from/to 없으면 전체 내역을 반환한다")
    void getPayments_noFilter() throws Exception {
        Payment p1 = savedPayment(1L);
        Payment p2 = savedPayment(2L);
        given(paymentRepository.findByUserNoOrderByPaidDesc(USER_NO)).willReturn(List.of(p1, p2));

        List<PaymentResponse> result = paymentService.getPayments(USER_NO, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("결제 목록 조회 - from/to 있으면 기간 필터링 결과를 반환한다")
    void getPayments_withDateFilter() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 1, 31, 23, 59);
        Payment p1 = savedPayment(1L);
        given(paymentRepository.findByUserNoAndPaidBetweenOrderByPaidDesc(USER_NO, from, to))
                .willReturn(List.of(p1));

        List<PaymentResponse> result = paymentService.getPayments(USER_NO, from, to);

        assertThat(result).hasSize(1);
        verify(paymentRepository).findByUserNoAndPaidBetweenOrderByPaidDesc(USER_NO, from, to);
    }

    // ── getPayment ───────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 단건 조회 - 결제 ID로 조회 시 PaymentResponse를 반환한다")
    void getPayment_success() throws Exception {
        Payment p = savedPayment(1L);
        given(paymentRepository.findById(1L)).willReturn(Optional.of(p));

        PaymentResponse result = paymentService.getPayment(USER_NO, 1L);

        assertThat(result.getPaymentId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("결제 단건 조회 - 존재하지 않는 ID면 NotFoundException")
    void getPayment_notFound() {
        given(paymentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(USER_NO, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 결제 내역입니다.");
    }
}
