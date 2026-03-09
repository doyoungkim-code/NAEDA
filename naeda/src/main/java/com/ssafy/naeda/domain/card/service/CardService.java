package com.ssafy.naeda.domain.card.service;

import com.ssafy.naeda.domain.card.dto.request.CardRegisterRequest;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.entity.DebitCard;
import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.payment.entity.MethodType;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private static final String API_PATH = "/edu/creditCard/createCreditCard";
    private static final String API_NAME = "createCreditCard";

    /** SSAFY cardTypeCode: "1" = 신용카드, "2" = 체크카드 */
    private static final String CREDIT_TYPE_CODE = "1";

    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final AccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;
    private final DebitCardRepository debitCardRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    /**
     * 카드 등록.
     *
     * 1) SSAFY createCreditCard API 호출
     * 2) 응답의 cardTypeCode에 따라 credit_card / debit_card 분기 저장
     * 3) payment_method 자동 생성
     *
     * @param userNo  우리 DB의 user PK
     * @param userKey SSAFY API userKey
     * @param request 카드 등록 요청 DTO
     * @return 카드 등록 결과
     */
    @Transactional
    public CardRegisterResponse registerCard(Long userNo, String userKey, CardRegisterRequest request) {

        // ── 1. SSAFY API 호출 ──
        Map<String, Object> header = ssafyHeaderFactory.create(API_NAME, userKey);
        Map<String, Object> body = ssafyApiClient.buildBody(header,
                "cardUniqueNo", request.getCardUniqueNo(),
                "withdrawalAccountNo", request.getWithdrawalAccountNo(),
                "withdrawalDate", request.getWithdrawalDate()
        );

        Map<String, Object> response = ssafyApiClient.post(API_PATH, body);

        @SuppressWarnings("unchecked")
        Map<String, Object> rec = (Map<String, Object>) response.get("REC");

        // ── 2. 공통 필드 추출 ──
        String cardNo         = (String) rec.get("cardNo");
        String cvc            = (String) rec.get("cvc");
        String cardUniqueNo   = (String) rec.get("cardUniqueNo");
        String cardIssuerCode = (String) rec.get("cardIssuerCode");
        String cardIssuerName = (String) rec.get("cardIssuerName");
        String cardName       = (String) rec.get("cardName");
        String cardExpiryDate = (String) rec.get("cardExpiryDate");
        String cardTypeCode   = (String) rec.get("cardTypeCode");
        String withdrawalAccountNo = (String) rec.get("withdrawalAccountNo");
        String withdrawalDate      = (String) rec.get("withdrawalDate");

        // ── 3. 중복 체크 ──
        if (creditCardRepository.existsByCardNo(cardNo) || debitCardRepository.existsByCardNo(cardNo)) {
            throw new DuplicateException("이미 등록된 카드입니다: " + cardNo);
        }

        // ── 4. cardTypeCode 분기 저장 + PaymentMethod 생성 ──
        if (CREDIT_TYPE_CODE.equals(cardTypeCode)) {
            return registerCreditCard(userNo, cardNo, cvc, cardUniqueNo,
                    cardIssuerCode, cardIssuerName, cardName, cardExpiryDate,
                    rec, withdrawalAccountNo, withdrawalDate);
        } else {
            return registerDebitCard(userNo, cardNo, cvc, cardUniqueNo,
                    cardIssuerCode, cardIssuerName, cardName, cardExpiryDate,
                    withdrawalAccountNo, withdrawalDate);
        }
    }

    /**
     * 신용카드 저장 + PaymentMethod 생성
     */
    private CardRegisterResponse registerCreditCard(Long userNo, String cardNo, String cvc,
                                                    String cardUniqueNo, String cardIssuerCode,
                                                    String cardIssuerName, String cardName,
                                                    String cardExpiryDate, Map<String, Object> rec,
                                                    String withdrawalAccountNo, String withdrawalDate) {

        // creditLimit: SSAFY 응답의 maxBenefitLimit 사용, 없으면 기본값 0
        Long creditLimit = parseLongOrDefault(rec.get("maxBenefitLimit"), 0L);
        Integer billingDate = parseIntOrDefault(withdrawalDate, 1);

        // withdrawalAccountNo로 Account 조회하여 accountId 매핑
        Account account = accountRepository.findByAccountNo(withdrawalAccountNo)
                .orElseThrow(() -> new NotFoundException("연결 계좌를 찾을 수 없습니다: " + withdrawalAccountNo));
        Long accountId = account.getAccountId();

        CreditCard creditCard = CreditCard.builder()
                .userNo(userNo)
                .cardNo(cardNo)
                .cvc(cvc)
                .cardUniqueNo(cardUniqueNo)
                .cardIssuerCode(cardIssuerCode)
                .cardIssuerName(cardIssuerName)
                .cardName(cardName)
                .cardExpiryDate(cardExpiryDate)
                .creditLimit(creditLimit)
                .billingDate(billingDate)
                .accountId(accountId)
                .build();

        creditCard = creditCardRepository.save(creditCard);
        log.info("[CardService] 신용카드 등록 완료: userNo={}, cardNo={}", userNo, cardNo);

        // PaymentMethod 자동 생성
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .userNo(userNo)
                .methodType(MethodType.CREDIT_CARD)
                .creditCardId(creditCard.getCreditCardId())
                .build();

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("[CardService] 결제수단 생성: paymentMethodId={}", paymentMethod.getPaymentMethodId());

        return CardRegisterResponse.fromCreditCard(
                creditCard, withdrawalAccountNo, withdrawalDate, paymentMethod.getPaymentMethodId());
    }

    /**
     * 체크카드 저장 + PaymentMethod 생성
     */
    private CardRegisterResponse registerDebitCard(Long userNo, String cardNo, String cvc,
                                                   String cardUniqueNo, String cardIssuerCode,
                                                   String cardIssuerName, String cardName,
                                                   String cardExpiryDate,
                                                   String withdrawalAccountNo, String withdrawalDate) {

        // withdrawalAccountNo로 Account 조회하여 accountId 매핑
        Account account = accountRepository.findByAccountNo(withdrawalAccountNo)
                .orElseThrow(() -> new NotFoundException("연결 계좌를 찾을 수 없습니다: " + withdrawalAccountNo));
        Long accountId = account.getAccountId();

        DebitCard debitCard = DebitCard.builder()
                .userNo(userNo)
                .cardNo(cardNo)
                .cvc(cvc)
                .cardUniqueNo(cardUniqueNo)
                .cardIssuerCode(cardIssuerCode)
                .cardIssuerName(cardIssuerName)
                .cardName(cardName)
                .cardExpiryDate(cardExpiryDate)
                .accountId(accountId)
                .build();

        debitCard = debitCardRepository.save(debitCard);
        log.info("[CardService] 체크카드 등록 완료: userNo={}, cardNo={}", userNo, cardNo);

        // PaymentMethod 자동 생성
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .userNo(userNo)
                .methodType(MethodType.DEBIT_CARD)
                .debitCardId(debitCard.getDebitCardId())
                .build();

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("[CardService] 결제수단 생성: paymentMethodId={}", paymentMethod.getPaymentMethodId());

        return CardRegisterResponse.fromDebitCard(
                debitCard, withdrawalAccountNo, withdrawalDate, paymentMethod.getPaymentMethodId());
    }

    // ── 유틸 ──

    private Long parseLongOrDefault(Object value, Long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseIntOrDefault(String value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}