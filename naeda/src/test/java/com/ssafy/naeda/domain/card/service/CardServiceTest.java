package com.ssafy.naeda.domain.card.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.card.dto.request.CardRegisterRequest;
import com.ssafy.naeda.domain.card.dto.request.CardTransactionRequest;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.dto.response.CardTransactionResponse;
import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.entity.DebitCard;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.payment.entity.MethodType;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private SsafyApiClient ssafyApiClient;
    @Mock private SsafyHeaderFactory ssafyHeaderFactory;
    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private DebitCardRepository debitCardRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private CardService cardService;

    private static final Long USER_NO = 1L;
    private static final String USER_KEY = "test-user-key";
    private static final String WITHDRAWAL_ACCOUNT_NO = "0320000000001234";

    private Account stubAccount;
    private User stubUser;

    @BeforeEach
    void setUp() {
        stubAccount = Account.builder()
                .userNo(USER_NO)
                .bankCode("032").bankName("부산은행")
                .accountNo(WITHDRAWAL_ACCOUNT_NO).accountName("내 계좌")
                .build();
        stubUser = User.builder()
                .userNo(USER_NO)
                .userId("test@example.com")
                .password("pw")
                .username("테스터")
                .residentNo("9901011")
                .phone("010-0000-0000")
                .institutionCode("M220516185630")
                .userKey(USER_KEY)
                .build();
    }

    // ── 헬퍼 ──

    private CardRegisterRequest createRequest() throws Exception {
        CardRegisterRequest request = new CardRegisterRequest();
        setField(request, "cardUniqueNo", "1003-unique-abc");
        setField(request, "withdrawalAccountNo", WITHDRAWAL_ACCOUNT_NO);
        setField(request, "withdrawalDate", "15");
        setField(request, "cardTypeCode", "1");
        return request;
    }

    private CardRegisterRequest createDebitRequest() throws Exception {
        CardRegisterRequest request = new CardRegisterRequest();
        setField(request, "cardUniqueNo", "1003-unique-abc");
        setField(request, "withdrawalAccountNo", WITHDRAWAL_ACCOUNT_NO);
        setField(request, "withdrawalDate", "15");
        setField(request, "cardTypeCode", "2");
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Map<String, Object> buildSsafyResponse() {
        Map<String, Object> rec = new HashMap<>();
        rec.put("cardNo", "1003622654847049");
        rec.put("cvc", "713");
        rec.put("cardUniqueNo", "1003-unique-abc");
        rec.put("cardIssuerCode", "1003");
        rec.put("cardIssuerName", "롯데카드");
        rec.put("cardName", "디지로카 SEOUL");
        rec.put("cardExpiryDate", "20290409");
        // cardTypeCode는 API 25 응답에 포함되지 않음 — request.getCardTypeCode()에서 읽음
        rec.put("withdrawalAccountNo", WITHDRAWAL_ACCOUNT_NO);
        rec.put("withdrawalDate", "15");
        rec.put("maxBenefitLimit", "200000");
        return Map.of("REC", rec);
    }

    private void stubCommonMocks() {
        given(userRepository.findById(USER_NO)).willReturn(Optional.of(stubUser));
        given(ssafyHeaderFactory.create(anyString(), anyString())).willReturn(Map.of());
        lenient().when(ssafyApiClient.buildBody(anyMap(), (Object[]) any())).thenReturn(Map.of());
    }

    private void stubCreditCardSave() {
        given(creditCardRepository.save(any(CreditCard.class))).willAnswer(invocation -> {
            CreditCard card = invocation.getArgument(0);
            setField(card, "creditCardId", 100L);
            return card;
        });
    }

    private void stubDebitCardSave() {
        given(debitCardRepository.save(any(DebitCard.class))).willAnswer(invocation -> {
            DebitCard card = invocation.getArgument(0);
            setField(card, "debitCardId", 200L);
            return card;
        });
    }

    private void stubPaymentMethodSave() {
        given(paymentMethodRepository.save(any(PaymentMethod.class))).willAnswer(invocation -> {
            PaymentMethod pm = invocation.getArgument(0);
            setField(pm, "paymentMethodId", 50L);
            return pm;
        });
    }

    private void stubAccountLookup() {
        given(accountRepository.findByAccountNo(WITHDRAWAL_ACCOUNT_NO)).willReturn(Optional.of(stubAccount));
    }

    // ── 신용카드 등록 성공 ──

    @Test
    @DisplayName("신용카드 등록 성공 - cardTypeCode '1'이면 CreditCard 저장 및 PaymentMethod 생성")
    void registerCard_creditCard_success() throws Exception {
        CardRegisterRequest request = createRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse());
        given(creditCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(debitCardRepository.existsByCardNo(anyString())).willReturn(false);
        stubAccountLookup();
        stubCreditCardSave();
        stubPaymentMethodSave();

        CardRegisterResponse result = cardService.registerCard(USER_NO, request);

        assertThat(result.getCardNo()).isEqualTo("1003622654847049");
        assertThat(result.getCvc()).isEqualTo("713");
        assertThat(result.getCardIssuerName()).isEqualTo("롯데카드");
        assertThat(result.getCardType()).isEqualTo("CREDIT");
        assertThat(result.getWithdrawalAccountNo()).isEqualTo(WITHDRAWAL_ACCOUNT_NO);
        assertThat(result.getWithdrawalDate()).isEqualTo("15");
        assertThat(result.getPaymentMethodId()).isEqualTo(50L);

        ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository).save(cardCaptor.capture());
        CreditCard savedCard = cardCaptor.getValue();
        assertThat(savedCard.getUserNo()).isEqualTo(USER_NO);
        assertThat(savedCard.getCreditLimit()).isEqualTo(200_000L);
        assertThat(savedCard.getBillingDate()).isEqualTo(15);

        ArgumentCaptor<PaymentMethod> pmCaptor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepository).save(pmCaptor.capture());
        assertThat(pmCaptor.getValue().getMethodType()).isEqualTo(MethodType.CREDIT_CARD);
    }

    // ── 체크카드 등록 성공 ──

    @Test
    @DisplayName("체크카드 등록 성공 - cardTypeCode '2'이면 DebitCard 저장 및 PaymentMethod 생성")
    void registerCard_debitCard_success() throws Exception {
        CardRegisterRequest request = createDebitRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse());
        given(creditCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(debitCardRepository.existsByCardNo(anyString())).willReturn(false);
        stubAccountLookup();
        stubDebitCardSave();
        stubPaymentMethodSave();

        CardRegisterResponse result = cardService.registerCard(USER_NO, request);

        assertThat(result.getCardType()).isEqualTo("DEBIT");
        assertThat(result.getCardNo()).isEqualTo("1003622654847049");
        assertThat(result.getPaymentMethodId()).isEqualTo(50L);

        ArgumentCaptor<DebitCard> cardCaptor = ArgumentCaptor.forClass(DebitCard.class);
        verify(debitCardRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getUserNo()).isEqualTo(USER_NO);

        ArgumentCaptor<PaymentMethod> pmCaptor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepository).save(pmCaptor.capture());
        assertThat(pmCaptor.getValue().getMethodType()).isEqualTo(MethodType.DEBIT_CARD);
    }

    // ── 중복 카드 ──

    @Test
    @DisplayName("카드 등록 실패 - 이미 등록된 신용카드이면 DuplicateException")
    void registerCard_duplicateCreditCard() throws Exception {
        CardRegisterRequest request = createRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse());
        given(creditCardRepository.existsByCardNo("1003622654847049")).willReturn(true);

        assertThatThrownBy(() -> cardService.registerCard(USER_NO, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("이미 등록된 카드입니다");
    }

    @Test
    @DisplayName("카드 등록 실패 - 이미 등록된 체크카드이면 DuplicateException")
    void registerCard_duplicateDebitCard() throws Exception {
        CardRegisterRequest request = createDebitRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse());
        given(creditCardRepository.existsByCardNo("1003622654847049")).willReturn(false);
        given(debitCardRepository.existsByCardNo("1003622654847049")).willReturn(true);

        assertThatThrownBy(() -> cardService.registerCard(USER_NO, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("이미 등록된 카드입니다");
    }

    // ── 연결 계좌 없음 ──

    @Test
    @DisplayName("카드 등록 실패 - 연결 계좌를 찾을 수 없으면 NotFoundException")
    void registerCard_accountNotFound() throws Exception {
        CardRegisterRequest request = createRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse());
        given(creditCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(debitCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(accountRepository.findByAccountNo(WITHDRAWAL_ACCOUNT_NO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.registerCard(USER_NO, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("연결 계좌를 찾을 수 없습니다");
    }

    // ── maxBenefitLimit 파싱 ──

    @Test
    @DisplayName("신용카드 등록 - maxBenefitLimit이 null이면 creditLimit을 0으로 저장")
    void registerCard_nullMaxBenefitLimit() throws Exception {
        CardRegisterRequest request = createRequest();

        Map<String, Object> rec = new HashMap<>();
        rec.put("cardNo", "1003622654847049");
        rec.put("cvc", "713");
        rec.put("cardUniqueNo", "1003-unique-abc");
        rec.put("cardIssuerCode", "1003");
        rec.put("cardIssuerName", "롯데카드");
        rec.put("cardName", "디지로카 SEOUL");
        rec.put("cardExpiryDate", "20290409");
        rec.put("withdrawalAccountNo", WITHDRAWAL_ACCOUNT_NO);
        rec.put("withdrawalDate", "15");
        rec.put("maxBenefitLimit", null);
        Map<String, Object> response = Map.of("REC", rec);

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(response);
        given(creditCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(debitCardRepository.existsByCardNo(anyString())).willReturn(false);
        stubAccountLookup();
        stubCreditCardSave();
        stubPaymentMethodSave();

        cardService.registerCard(USER_NO, request);

        ArgumentCaptor<CreditCard> captor = ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository).save(captor.capture());
        assertThat(captor.getValue().getCreditLimit()).isEqualTo(0L);
    }

    // ── getMyCards ──

    private CreditCard buildCreditCard(Long id, Long userNo, String cardNo) throws Exception {
        CreditCard card = CreditCard.builder()
                .userNo(userNo).cardNo(cardNo).cvc("123")
                .cardUniqueNo("1003-unique-" + cardNo)
                .cardIssuerCode("1003").cardIssuerName("롯데카드")
                .cardName("테스트 신용카드").cardExpiryDate("20290101")
                .creditLimit(1_000_000L).billingDate(15).accountId(1L)
                .build();
        setField(card, "creditCardId", id);
        return card;
    }

    private DebitCard buildDebitCard(Long id, Long userNo, String cardNo) throws Exception {
        DebitCard card = DebitCard.builder()
                .userNo(userNo).cardNo(cardNo).cvc("456")
                .cardUniqueNo("1005-unique-" + cardNo)
                .cardIssuerCode("1005").cardIssuerName("신한카드")
                .cardName("테스트 체크카드").cardExpiryDate("20290101")
                .accountId(1L)
                .build();
        setField(card, "debitCardId", id);
        return card;
    }

    @Test
    @DisplayName("내 카드 목록 조회 - 신용카드 + 체크카드 합쳐서 반환")
    void getMyCards_success() throws Exception {
        given(creditCardRepository.findByUserNoAndIsActiveTrue(USER_NO))
                .willReturn(List.of(buildCreditCard(1L, USER_NO, "1003000000001111")));
        given(debitCardRepository.findByUserNoAndIsActiveTrue(USER_NO))
                .willReturn(List.of(buildDebitCard(2L, USER_NO, "1005000000002222")));

        var cards = cardService.getMyCards(USER_NO);

        assertThat(cards).hasSize(2);
        assertThat(cards).extracting("cardType").containsExactly("CREDIT", "DEBIT");
    }

    @Test
    @DisplayName("내 카드 목록 조회 - 카드가 없으면 빈 리스트 반환")
    void getMyCards_empty() {
        given(creditCardRepository.findByUserNoAndIsActiveTrue(USER_NO)).willReturn(List.of());
        given(debitCardRepository.findByUserNoAndIsActiveTrue(USER_NO)).willReturn(List.of());

        var cards = cardService.getMyCards(USER_NO);

        assertThat(cards).isEmpty();
    }

    // ── deleteCard ──

    @Test
    @DisplayName("신용카드 삭제 성공 - 비활성화 및 PaymentMethod 삭제")
    void deleteCard_creditCard_success() throws Exception {
        CreditCard card = buildCreditCard(100L, USER_NO, "1003000000001111");
        given(creditCardRepository.findById(100L)).willReturn(Optional.of(card));
        given(paymentMethodRepository.findByCreditCardId(100L)).willReturn(List.of(
                PaymentMethod.builder().userNo(USER_NO).methodType(MethodType.CREDIT_CARD).creditCardId(100L).build()
        ));

        cardService.deleteCard(USER_NO, 100L, "CREDIT");

        assertThat(card.getIsActive()).isFalse();
        verify(paymentMethodRepository).deleteAll(anyList());
    }

    @Test
    @DisplayName("체크카드 삭제 성공 - 비활성화 및 PaymentMethod 삭제")
    void deleteCard_debitCard_success() throws Exception {
        DebitCard card = buildDebitCard(200L, USER_NO, "1005000000002222");
        given(debitCardRepository.findById(200L)).willReturn(Optional.of(card));
        given(paymentMethodRepository.findByDebitCardId(200L)).willReturn(List.of(
                PaymentMethod.builder().userNo(USER_NO).methodType(MethodType.DEBIT_CARD).debitCardId(200L).build()
        ));

        cardService.deleteCard(USER_NO, 200L, "DEBIT");

        assertThat(card.getIsActive()).isFalse();
        verify(paymentMethodRepository).deleteAll(anyList());
    }

    @Test
    @DisplayName("카드 삭제 실패 - 카드를 찾을 수 없으면 NotFoundException")
    void deleteCard_notFound() {
        given(creditCardRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.deleteCard(USER_NO, 999L, "CREDIT"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("카드를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("카드 삭제 실패 - 소유자가 다르면 NotFoundException")
    void deleteCard_ownershipMismatch() throws Exception {
        CreditCard card = buildCreditCard(100L, 999L, "1003000000001111");
        given(creditCardRepository.findById(100L)).willReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.deleteCard(USER_NO, 100L, "CREDIT"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("카드를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("카드 삭제 실패 - 잘못된 카드 타입이면 IllegalArgumentException")
    void deleteCard_invalidCardType() {
        assertThatThrownBy(() -> cardService.deleteCard(USER_NO, 100L, "INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 카드 타입입니다");
    }

    // ── getCardTransactions ──

    private CardTransactionRequest createTransactionRequest() throws Exception {
        CardTransactionRequest request = new CardTransactionRequest();
        setField(request, "startDate", "20240401");
        setField(request, "endDate", "20240430");
        return request;
    }

    private Map<String, Object> buildSsafyTransactionResponse(int count) {
        List<Map<String, Object>> txList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> tx = new HashMap<>();
            tx.put("transactionUniqueNo", "TX-" + i);
            tx.put("categoryName", "식비");
            tx.put("merchantName", "스타벅스");
            tx.put("transactionDate", "20240410");
            tx.put("transactionTime", "143000");
            tx.put("transactionBalance", "5000");
            tx.put("cardStatus", "승인");
            txList.add(tx);
        }
        Map<String, Object> rec = new HashMap<>();
        rec.put("transactionList", txList);
        return Map.of("REC", rec);
    }

    private void stubTransactionLogSave() {
        given(transactionLogRepository.save(any(TransactionLog.class))).willAnswer(invocation -> {
            TransactionLog log = invocation.getArgument(0);
            setField(log, "logId", 1L);
            return log;
        });
    }

    @Test
    @DisplayName("카드 결제 내역 조회 성공 - 신용카드 거래내역 조회 및 DB 저장")
    void getCardTransactions_creditCard_success() throws Exception {
        CreditCard card = buildCreditCard(100L, USER_NO, "1003000000001111");
        given(creditCardRepository.findById(100L)).willReturn(Optional.of(card));

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyTransactionResponse(2));
        given(transactionLogRepository.findBySsafyTransactionId(anyString())).willReturn(Optional.empty());
        stubTransactionLogSave();

        List<CardTransactionResponse> result = cardService.getCardTransactions(USER_NO, USER_KEY, 100L, createTransactionRequest());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryName()).isEqualTo("식비");
        assertThat(result.get(0).getMerchantName()).isEqualTo("스타벅스");
        assertThat(result.get(0).getAmount()).isEqualTo(5000L);
        verify(transactionLogRepository, times(2)).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("카드 결제 내역 조회 성공 - 체크카드 거래내역 조회")
    void getCardTransactions_debitCard_success() throws Exception {
        given(creditCardRepository.findById(200L)).willReturn(Optional.empty());
        DebitCard card = buildDebitCard(200L, USER_NO, "1005000000002222");
        given(debitCardRepository.findById(200L)).willReturn(Optional.of(card));

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyTransactionResponse(1));
        given(transactionLogRepository.findBySsafyTransactionId(anyString())).willReturn(Optional.empty());
        stubTransactionLogSave();

        List<CardTransactionResponse> result = cardService.getCardTransactions(USER_NO, USER_KEY, 200L, createTransactionRequest());

        assertThat(result).hasSize(1);
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("카드 결제 내역 조회 - 빈 거래 목록이면 빈 리스트 반환")
    void getCardTransactions_emptyList() throws Exception {
        CreditCard card = buildCreditCard(100L, USER_NO, "1003000000001111");
        given(creditCardRepository.findById(100L)).willReturn(Optional.of(card));

        stubCommonMocks();
        Map<String, Object> rec = new HashMap<>();
        rec.put("transactionList", List.of());
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(Map.of("REC", rec));

        List<CardTransactionResponse> result = cardService.getCardTransactions(USER_NO, USER_KEY, 100L, createTransactionRequest());

        assertThat(result).isEmpty();
        verify(transactionLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("카드 결제 내역 조회 실패 - 카드를 찾을 수 없으면 NotFoundException")
    void getCardTransactions_cardNotFound() throws Exception {
        given(creditCardRepository.findById(999L)).willReturn(Optional.empty());
        given(debitCardRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardTransactions(USER_NO, USER_KEY, 999L, createTransactionRequest()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("카드를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("카드 결제 내역 조회 - 이미 캐싱된 거래는 DB에서 조회하고 새로 저장하지 않음")
    void getCardTransactions_existingTransaction_noSave() throws Exception {
        CreditCard card = buildCreditCard(100L, USER_NO, "1003000000001111");
        given(creditCardRepository.findById(100L)).willReturn(Optional.of(card));

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyTransactionResponse(1));

        TransactionLog existingLog = TransactionLog.builder()
                .accountId(1L).transactionType(TransactionType.WITHDRAW)
                .amount(5000L).balanceAfter(0L)
                .counterpart("스타벅스").category("식비").memo("승인")
                .ssafyTransactionId("TX-0")
                .transacted(LocalDateTime.of(2024, 4, 10, 14, 30, 0))
                .build();
        setField(existingLog, "logId", 10L);

        given(transactionLogRepository.findBySsafyTransactionId("TX-0")).willReturn(Optional.of(existingLog));

        List<CardTransactionResponse> result = cardService.getCardTransactions(USER_NO, USER_KEY, 100L, createTransactionRequest());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLogId()).isEqualTo(10L);
        verify(transactionLogRepository, never()).save(any(TransactionLog.class));
    }
}
