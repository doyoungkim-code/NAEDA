package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.service.FaceService;
import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.payment.dto.request.ProcessPaymentRequest;
import com.ssafy.naeda.domain.payment.dto.response.ProcessPaymentResponse;
import com.ssafy.naeda.domain.payment.entity.*;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import com.ssafy.naeda.domain.payment.repository.PaymentRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    // ── 성공 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 성공 - PASS일 때 이체 + Payment 저장 + 포인트 적립")
    void processPayment_success() {
        // given
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

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
        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, request, faceImage);

        // then
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getNextAction()).isEqualTo("PASS");
        assertThat(response.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(response.getStoreId()).isEqualTo(STORE_ID);
        assertThat(response.getAmount()).isEqualTo(AMOUNT);
        verify(redisService).updateStatus(REQUEST_ID, PaymentRequestStatus.PROCESSING);
        verify(redisService).updateStatus(REQUEST_ID, PaymentRequestStatus.SUCCESS);
    }

    // ── 요청 만료/미존재 ──────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 요청이 만료되었거나 존재하지 않으면 NotFoundException")
    void processPayment_requestNotFound() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        given(redisService.getRequest(REQUEST_ID)).willReturn(null);

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, request, faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("결제 요청이 만료되었거나 존재하지 않습니다");
    }

    // ── 상태 불일치 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - PENDING이 아니면 BadRequestException")
    void processPayment_notPending() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PROCESSING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, request, faceImage))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이미 처리 중이거나 완료된 결제 요청입니다");
    }

    // ── 얼굴 차단 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 - 얼굴 인증 차단 시 BLOCKED 반환")
    void processPayment_blocked() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

        Store store = Store.builder().storeId(STORE_ID).storeName("테스트 매장").build();
        given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

        SearchResponse faceResult = SearchResponse.builder()
                .matched(false).nextAction("BLOCK").bestUserId(null)
                .similarity(0.3f).blocked(true).authLevel(AuthLevel.BLOCKED).build();
        given(faceService.search(any(), eq(1), eq(AMOUNT))).willReturn(faceResult);

        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, request, faceImage);

        assertThat(response.getStatus()).isEqualTo("BLOCKED");
        assertThat(response.getNextAction()).isEqualTo("BLOCK");
        verify(redisService).updateStatus(REQUEST_ID, PaymentRequestStatus.BLOCKED);
    }

    // ── 2차 인증 필요 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 - 2차 인증 필요 시 FAILED + nextAction 반환")
    void processPayment_requireSecondFactor() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

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

        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, request, faceImage);

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
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

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

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, request, faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("페이스페이 결제 수단이 등록되지 않았습니다");
    }

    // ── ACCOUNT 타입이 아닌 경우 ──────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 결제수단이 ACCOUNT가 아니면 BadRequestException")
    void processPayment_notAccountType() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

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

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, request, faceImage))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("계좌 결제 수단만 지원합니다");
    }

    // ── 출금 계좌 미존재 ──────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 출금 계좌를 찾을 수 없으면 NotFoundException")
    void processPayment_withdrawalAccountNotFound() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

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

        given(accountRepository.findById(300L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, request, faceImage))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("출금 계좌를 찾을 수 없습니다");
    }

    // ── SSAFY API 실패 ───────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - SSAFY 이체 API 오류 시 FAILED 반환")
    void processPayment_ssafyApiFailed() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

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

        Account withdrawalAccount = Account.builder().accountId(300L).accountNo("111-222-333").build();
        Account depositAccount = Account.builder().accountId(200L).accountNo("444-555-666").build();
        given(accountRepository.findById(300L)).willReturn(Optional.of(withdrawalAccount));
        given(accountRepository.findById(200L)).willReturn(Optional.of(depositAccount));

        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        given(ssafyApiClient.buildBody(any(Map.class), any(Object[].class))).willReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/demandDeposit/updateDemandDepositAccountTransfer"), any()))
                .willThrow(new RuntimeException("SSAFY 서버 오류"));

        ProcessPaymentResponse response = service.processPayment(REQUEST_ID, request, faceImage);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureReason()).contains("SSAFY API 오류");
    }

    // ── 중복 거래 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 처리 실패 - 이미 처리된 거래 ID이면 BadRequestException")
    void processPayment_duplicateTransaction() {
        MultipartFile faceImage = mock(MultipartFile.class);
        ProcessPaymentRequest request = mock(ProcessPaymentRequest.class);

        PaymentRequestData data = PaymentRequestData.builder()
                .requestId(REQUEST_ID).storeId(STORE_ID).amount(AMOUNT)
                .status(PaymentRequestStatus.PENDING).build();
        given(redisService.getRequest(REQUEST_ID)).willReturn(data);

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

        assertThatThrownBy(() -> service.processPayment(REQUEST_ID, request, faceImage))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이미 처리된 결제입니다");
    }
}
