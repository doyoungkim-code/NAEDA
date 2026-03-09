package com.ssafy.naeda.domain.card.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.card.dto.request.CardRegisterRequest;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.entity.DebitCard;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.payment.entity.MethodType;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private SsafyApiClient ssafyApiClient;
    @Mock private SsafyHeaderFactory ssafyHeaderFactory;
    @Mock private AccountRepository accountRepository;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private DebitCardRepository debitCardRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private CardService cardService;

    private static final Long USER_NO = 1L;
    private static final String USER_KEY = "test-user-key";
    private static final String WITHDRAWAL_ACCOUNT_NO = "0320000000001234";

    private Account stubAccount;

    @BeforeEach
    void setUp() {
        stubAccount = Account.builder()
                .userNo(USER_NO)
                .bankCode("032").bankName("부산은행")
                .accountNo(WITHDRAWAL_ACCOUNT_NO).accountName("내 계좌")
                .build();
    }

    // ── 헬퍼 ──

    private CardRegisterRequest createRequest() throws Exception {
        CardRegisterRequest request = new CardRegisterRequest();
        setField(request, "cardUniqueNo", "1003-unique-abc");
        setField(request, "withdrawalAccountNo", WITHDRAWAL_ACCOUNT_NO);
        setField(request, "withdrawalDate", "15");
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Map<String, Object> buildSsafyResponse(String cardTypeCode) {
        Map<String, Object> rec = new HashMap<>();
        rec.put("cardNo", "1003622654847049");
        rec.put("cvc", "713");
        rec.put("cardUniqueNo", "1003-unique-abc");
        rec.put("cardIssuerCode", "1003");
        rec.put("cardIssuerName", "롯데카드");
        rec.put("cardName", "디지로카 SEOUL");
        rec.put("cardExpiryDate", "20290409");
        rec.put("cardTypeCode", cardTypeCode);
        rec.put("withdrawalAccountNo", WITHDRAWAL_ACCOUNT_NO);
        rec.put("withdrawalDate", "15");
        rec.put("maxBenefitLimit", "200000");
        return Map.of("REC", rec);
    }

    private void stubCommonMocks() {
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
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse("1"));
        given(creditCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(debitCardRepository.existsByCardNo(anyString())).willReturn(false);
        stubAccountLookup();
        stubCreditCardSave();
        stubPaymentMethodSave();

        CardRegisterResponse result = cardService.registerCard(USER_NO, USER_KEY, request);

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
        CardRegisterRequest request = createRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse("2"));
        given(creditCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(debitCardRepository.existsByCardNo(anyString())).willReturn(false);
        stubAccountLookup();
        stubDebitCardSave();
        stubPaymentMethodSave();

        CardRegisterResponse result = cardService.registerCard(USER_NO, USER_KEY, request);

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
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse("1"));
        given(creditCardRepository.existsByCardNo("1003622654847049")).willReturn(true);

        assertThatThrownBy(() -> cardService.registerCard(USER_NO, USER_KEY, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("이미 등록된 카드입니다");
    }

    @Test
    @DisplayName("카드 등록 실패 - 이미 등록된 체크카드이면 DuplicateException")
    void registerCard_duplicateDebitCard() throws Exception {
        CardRegisterRequest request = createRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse("2"));
        given(creditCardRepository.existsByCardNo("1003622654847049")).willReturn(false);
        given(debitCardRepository.existsByCardNo("1003622654847049")).willReturn(true);

        assertThatThrownBy(() -> cardService.registerCard(USER_NO, USER_KEY, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("이미 등록된 카드입니다");
    }

    // ── 연결 계좌 없음 ──

    @Test
    @DisplayName("카드 등록 실패 - 연결 계좌를 찾을 수 없으면 NotFoundException")
    void registerCard_accountNotFound() throws Exception {
        CardRegisterRequest request = createRequest();

        stubCommonMocks();
        given(ssafyApiClient.post(anyString(), anyMap())).willReturn(buildSsafyResponse("1"));
        given(creditCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(debitCardRepository.existsByCardNo(anyString())).willReturn(false);
        given(accountRepository.findByAccountNo(WITHDRAWAL_ACCOUNT_NO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.registerCard(USER_NO, USER_KEY, request))
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
        rec.put("cardTypeCode", "1");
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

        cardService.registerCard(USER_NO, USER_KEY, request);

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
}
