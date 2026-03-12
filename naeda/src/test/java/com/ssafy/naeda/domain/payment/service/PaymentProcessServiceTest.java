package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.service.FaceService;
import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.dto.response.FdsEvaluationResult;
import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.service.FdsRuleService;
import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.payment.dto.response.ProcessPaymentResponse;
import com.ssafy.naeda.domain.payment.entity.*;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import com.ssafy.naeda.domain.payment.repository.PaymentRepository;
import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.service.PointService;
import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentProcessServiceTest {

    @Mock private PaymentRequestRedisService redisService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private FaceService faceService;
    @Mock private PointService pointService;
    @Mock private FdsRuleService fdsRuleService;
    @Mock private SsafyApiClient ssafyApiClient;
    @Mock private SsafyHeaderFactory ssafyHeaderFactory;

    @InjectMocks
    private PaymentProcessService service;

    private static final String REQUEST_ID = "test-request-id";
    private static final Long STORE_ID = 100L;
    private static final Long AMOUNT = 15000L;
    private static final Long USER_NO = 10L;
    private static final String USER_ID = "user-1001";
    private static final String USER_KEY = "test-user-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pointRate", 0.05);
    }

    // ── 성공 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 성공 - PASS일 때 이체 + Payment 저장 + 포인트 적립")
    void processPayment_success() {
        // given
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").accountId(200L).build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(true).nextAction("PASS").bestUserId(USER_ID)
                .similarity(0.95f).blocked(false).authLevel(AuthLevel.FACE_ONLY).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        User user = User.builder().userNo(USER_NO).userId(USER_ID).userKey(USER_KEY).build();
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));

        PaymentMethod pm = PaymentMethod.builder()
                .paymentMethodId(1L).userNo(USER_NO).methodType(MethodType.ACCOUNT).accountId(300L).build();
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(pm));

        // FDS 스텁
        given(paymentRepository.countByUserNoAndStatusAndPaidAfter(eq(USER_NO), eq(PaymentStatus.SUCCESS), any()))
                .willReturn(0);
        given(paymentRepository.sumAmountByUserNoAndPaidAfter(eq(USER_NO), any()))
                .willReturn(0L);
        FdsEvaluationResult fdsResult = new FdsEvaluationResult(0, List.of(), FdsAction.NONE);
        given(fdsRuleService.evaluate(any(FdsEvaluationRequest.class))).willReturn(fdsResult);

        Account withdrawalAccount = Account.builder().accountId(300L).accountNo("111-222-333").build();
        Account depositAccount = Account.builder().accountId(200L).accountNo("444-555-666").build();
        given(accountRepository.findById(300L)).willReturn(Optional.of(withdrawalAccount));
        given(accountRepository.findById(200L)).willReturn(Optional.of(depositAccount));

        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(any(Map.class), any(Object[].class))).willReturn(Map.of());

        Map<String, Object> transferRec = Map.of("transactionType", "2", "transactionUniqueNo", "TXN-001");
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), any()))
                .willReturn(Map.of("REC", List.of(transferRec)));
        given(paymentRepository.existsBySsafyTransactionId("TXN-001")).willReturn(false);

        Map<String, Object> balanceRec = Map.of("accountBalance", "85000");
        given(ssafyApiClient.post(eq("/edu/demandDeposit/inquireDemandDepositAccountBalance"), any()))
                .willReturn(Map.of("REC", balanceRec));

        Payment savedPayment = Payment.builder().paymentId(50L).userNo(USER_NO)
                .storeId(STORE_ID).paymentMethodId(1L).amount(AMOUNT)
                .authMethod(AuthMethod.FACE_PAY).authLevel(com.ssafy.naeda.domain.payment.entity.AuthLevel.FACE_ONLY)
                .build();
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // when
        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, faceImage);

        // then
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getNextAction()).isEqualTo("PASS");
        assertThat(response.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(response.getStoreId()).isEqualTo(STORE_ID);
        assertThat(response.getAmount()).isEqualTo(AMOUNT);
        assertThat(response.getEarnedPoints()).isEqualTo(750); // 15000 * 0.05
        assertThat(response.getFdsScore()).isEqualTo(0);
        assertThat(response.getFdsAction()).isEqualTo("NONE");
        verify(redisService).tryAcquireProcessingLock(REQUEST_ID);
        verify(redisService).updateStatus(REQUEST_ID, PaymentRequestStatus.PROCESSING);
        verify(redisService).updateStatus(REQUEST_ID, PaymentRequestStatus.SUCCESS);
        verify(pointService).earnPoints(eq(USER_NO), any(PointEarnRequest.class));
        verify(fdsRuleService).saveLog(eq(50L), eq(USER_NO), eq(fdsResult));
    }

    // ── 요청 만료/미존재 ──────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 요청이 만료되었거나 존재하지 않으면 NotFoundException")
    void processPayment_requestNotFound() {
        MultipartFile faceImage = mock(MultipartFile.class);

        given(redisService.getRequest(REQUEST_ID)).willReturn(null);

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("결제 요청이 만료되었거나 존재하지 않습니다");
    }

    // ── 상태 불일치 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - PENDING이 아니면 BadRequestException")
    void processPayment_notPending() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PROCESSING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, faceImage))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이미 처리 중이거나 완료된 결제 요청입니다");
    }

    // ── 얼굴 차단 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 - 얼굴 인증 차단 시 BLOCKED 반환")
    void processPayment_blocked() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(false).nextAction("BLOCK").bestUserId(null)
                .similarity(0.3f).blocked(true).authLevel(AuthLevel.BLOCKED).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, faceImage);

        assertThat(response.getStatus()).isEqualTo("BLOCKED");
        assertThat(response.getNextAction()).isEqualTo("BLOCK");
        verify(redisService).updateStatus(REQUEST_ID, PaymentRequestStatus.BLOCKED);
    }

    // ── 2차 인증 필요 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 - 2차 인증 필요 시 FAILED + nextAction 반환")
    void processPayment_requireSecondFactor() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(true).nextAction("REQUIRE_PIN").bestUserId(USER_ID)
                .similarity(0.75f).blocked(false).authLevel(AuthLevel.FACE_PIN).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        User user = User.builder().userNo(USER_NO).userId(USER_ID).userKey(USER_KEY).build();
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));

        PaymentMethod pm = PaymentMethod.builder()
                .paymentMethodId(1L).userNo(USER_NO).methodType(MethodType.ACCOUNT).accountId(300L).build();
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(pm));

        Payment savedPayment = Payment.builder().paymentId(50L).userNo(USER_NO)
                .storeId(STORE_ID).paymentMethodId(1L).amount(AMOUNT)
                .authMethod(AuthMethod.FACE_PAY).authLevel(com.ssafy.naeda.domain.payment.entity.AuthLevel.FACE_PIN)
                .build();
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, faceImage);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getNextAction()).isEqualTo("REQUIRE_PIN");
        assertThat(response.getPaymentId()).isEqualTo(50L);
        verify(redisService).updateStatus(REQUEST_ID, PaymentRequestStatus.FAILED);
    }

    // ── 결제수단 미존재 ───────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 페이스페이 결제수단이 없으면 NotFoundException")
    void processPayment_paymentMethodNotFound() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(true).nextAction("PASS").bestUserId(USER_ID)
                .similarity(0.95f).blocked(false).authLevel(AuthLevel.FACE_ONLY).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        User user = User.builder().userNo(USER_NO).userId(USER_ID).userKey(USER_KEY).build();
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));

        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("페이스페이 결제 수단이 등록되지 않았습니다");
    }

    // ── ACCOUNT 타입이 아닌 경우 ──────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 결제수단이 ACCOUNT가 아니면 BadRequestException")
    void processPayment_notAccountType() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(true).nextAction("PASS").bestUserId(USER_ID)
                .similarity(0.95f).blocked(false).authLevel(AuthLevel.FACE_ONLY).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        User user = User.builder().userNo(USER_NO).userId(USER_ID).userKey(USER_KEY).build();
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));

        PaymentMethod pm = PaymentMethod.builder()
                .paymentMethodId(1L).userNo(USER_NO).methodType(MethodType.DEBIT_CARD).build();
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(pm));

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, faceImage))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("계좌 결제 수단만 지원합니다");
    }

    // ── 출금 계좌 미존재 ──────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 출금 계좌를 찾을 수 없으면 NotFoundException")
    void processPayment_withdrawalAccountNotFound() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").accountId(200L).build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(true).nextAction("PASS").bestUserId(USER_ID)
                .similarity(0.95f).blocked(false).authLevel(AuthLevel.FACE_ONLY).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        User user = User.builder().userNo(USER_NO).userId(USER_ID).userKey(USER_KEY).build();
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));

        PaymentMethod pm = PaymentMethod.builder()
                .paymentMethodId(1L).userNo(USER_NO).methodType(MethodType.ACCOUNT).accountId(300L).build();
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(pm));

        // FDS 스텁
        given(paymentRepository.countByUserNoAndStatusAndPaidAfter(eq(USER_NO), eq(PaymentStatus.SUCCESS), any()))
                .willReturn(0);
        given(paymentRepository.sumAmountByUserNoAndPaidAfter(eq(USER_NO), any()))
                .willReturn(0L);
        given(fdsRuleService.evaluate(any(FdsEvaluationRequest.class)))
                .willReturn(new FdsEvaluationResult(0, List.of(), FdsAction.NONE));

        given(accountRepository.findById(300L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("출금 계좌를 찾을 수 없습니다");
    }

    // ── SSAFY API 실패 ───────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - SSAFY 이체 API 오류 시 FAILED 반환")
    void processPayment_ssafyApiFailed() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").accountId(200L).build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(true).nextAction("PASS").bestUserId(USER_ID)
                .similarity(0.95f).blocked(false).authLevel(AuthLevel.FACE_ONLY).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        User user = User.builder().userNo(USER_NO).userId(USER_ID).userKey(USER_KEY).build();
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));

        PaymentMethod pm = PaymentMethod.builder()
                .paymentMethodId(1L).userNo(USER_NO).methodType(MethodType.ACCOUNT).accountId(300L).build();
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(pm));

        // FDS 스텁
        given(paymentRepository.countByUserNoAndStatusAndPaidAfter(eq(USER_NO), eq(PaymentStatus.SUCCESS), any()))
                .willReturn(0);
        given(paymentRepository.sumAmountByUserNoAndPaidAfter(eq(USER_NO), any()))
                .willReturn(0L);
        given(fdsRuleService.evaluate(any(FdsEvaluationRequest.class)))
                .willReturn(new FdsEvaluationResult(0, List.of(), FdsAction.NONE));

        Account withdrawalAccount = Account.builder().accountId(300L).accountNo("111-222-333").build();
        Account depositAccount = Account.builder().accountId(200L).accountNo("444-555-666").build();
        given(accountRepository.findById(300L)).willReturn(Optional.of(withdrawalAccount));
        given(accountRepository.findById(200L)).willReturn(Optional.of(depositAccount));

        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(any(Map.class), any(Object[].class))).willReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), any()))
                .willThrow(new RuntimeException("SSAFY 서버 오류"));

        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, faceImage);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureReason()).contains("결제 처리 중 오류가 발생했습니다");
    }

    // ── 중복 거래 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 - 중복 거래 ID가 감지돼도 이체 이미 완료이므로 정상 처리")
    void processPayment_duplicateTransaction_stillSucceeds() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(true);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").accountId(200L).build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(true).nextAction("PASS").bestUserId(USER_ID)
                .similarity(0.95f).blocked(false).authLevel(AuthLevel.FACE_ONLY).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        User user = User.builder().userNo(USER_NO).userId(USER_ID).userKey(USER_KEY).build();
        given(userRepository.findByUserId(USER_ID)).willReturn(Optional.of(user));

        PaymentMethod pm = PaymentMethod.builder()
                .paymentMethodId(1L).userNo(USER_NO).methodType(MethodType.ACCOUNT).accountId(300L).build();
        given(paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(USER_NO))
                .willReturn(Optional.of(pm));

        // FDS 스텁
        given(paymentRepository.countByUserNoAndStatusAndPaidAfter(eq(USER_NO), eq(PaymentStatus.SUCCESS), any()))
                .willReturn(0);
        given(paymentRepository.sumAmountByUserNoAndPaidAfter(eq(USER_NO), any()))
                .willReturn(0L);
        FdsEvaluationResult fdsResult = new FdsEvaluationResult(0, List.of(), FdsAction.NONE);
        given(fdsRuleService.evaluate(any(FdsEvaluationRequest.class))).willReturn(fdsResult);

        Account withdrawalAccount = Account.builder().accountId(300L).accountNo("111-222-333").build();
        Account depositAccount = Account.builder().accountId(200L).accountNo("444-555-666").build();
        given(accountRepository.findById(300L)).willReturn(Optional.of(withdrawalAccount));
        given(accountRepository.findById(200L)).willReturn(Optional.of(depositAccount));

        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(any(Map.class), any(Object[].class))).willReturn(Map.of());

        Map<String, Object> transferRec = Map.of("transactionType", "2", "transactionUniqueNo", "TXN-DUP");
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), any()))
                .willReturn(Map.of("REC", List.of(transferRec)));
        given(paymentRepository.existsBySsafyTransactionId("TXN-DUP")).willReturn(true);

        Map<String, Object> balanceRec = Map.of("accountBalance", "85000");
        given(ssafyApiClient.post(eq("/edu/demandDeposit/inquireDemandDepositAccountBalance"), any()))
                .willReturn(Map.of("REC", balanceRec));

        Payment savedPayment = Payment.builder().paymentId(50L).userNo(USER_NO)
                .storeId(STORE_ID).paymentMethodId(1L).amount(AMOUNT)
                .authMethod(AuthMethod.FACE_PAY).authLevel(com.ssafy.naeda.domain.payment.entity.AuthLevel.FACE_ONLY)
                .build();
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, faceImage);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository).save(any(Payment.class));
        verify(fdsRuleService).saveLog(eq(50L), eq(USER_NO), eq(fdsResult));
    }

    // ── 동시 요청 (분산 락) ─────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 동시 요청 시 락 획득 실패하면 BadRequestException")
    void processPayment_concurrentRequest_lockFailed() {
        MultipartFile faceImage = mock(MultipartFile.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);
        given(redisService.tryAcquireProcessingLock(REQUEST_ID)).willReturn(false);

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, faceImage))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이미 처리 중이거나 완료된 결제 요청입니다");
    }
}
