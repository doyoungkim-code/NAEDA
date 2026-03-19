package com.ssafy.naeda.domain.card.service;

import com.ssafy.naeda.domain.card.dto.request.CardRegisterRequest;
import com.ssafy.naeda.domain.card.dto.request.CardTransactionRequest;
import com.ssafy.naeda.domain.card.dto.response.CardRegisterResponse;
import com.ssafy.naeda.domain.card.dto.response.CardResponse;
import com.ssafy.naeda.domain.card.dto.response.CardTransactionResponse;
import com.ssafy.naeda.domain.card.entity.CreditCard;
import com.ssafy.naeda.domain.card.entity.DebitCard;
import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.consumption.client.ConsumptionCategoryAiClient;
import com.ssafy.naeda.domain.consumption.client.ConsumptionCategoryFallbackMapper;
import com.ssafy.naeda.domain.consumption.client.dto.AiConsumptionCategoryItem;
import com.ssafy.naeda.domain.pay.entity.MethodType;
import com.ssafy.naeda.domain.pay.entity.PayMethod;
import com.ssafy.naeda.domain.pay.repository.PayMethodRepository;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    // 명세서 API 25: URL 경로는 createCreditCardProduct, Header apiName은 createCreditCard
    private static final String API_PATH = "/edu/creditCard/createCreditCard";
    private static final String API_NAME = "createCreditCard";

    private static final String CARD_TX_PATH = "/edu/creditCard/inquireCreditCardTransactionList";
    private static final String CARD_TX_API  = "inquireCreditCardTransactionList";

    /** SSAFY cardTypeCode: "1" = 신용카드, "2" = 체크카드 */
    private static final String CREDIT_TYPE_CODE = "1";

    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;
    private final DebitCardRepository debitCardRepository;
    private final PayMethodRepository paymentMethodRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final ConsumptionCategoryAiClient consumptionCategoryAiClient;

    /**
     * 카드 등록.
     *
     * 1) User 조회 → userKey 획득
     * 2) SSAFY createCreditCard API 호출
     * 3) request.cardTypeCode에 따라 credit_card / debit_card 분기 저장
     * 4) payment_method 자동 생성
     */
    @Transactional
    public CardRegisterResponse registerCard(Long userNo, CardRegisterRequest request) {

        // ── 1. User 조회 → userKey ──
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));

        // ── 2. SSAFY API 호출 ──
        Map<String, Object> header = ssafyHeaderFactory.create(API_NAME, user.getUserKey());
        Map<String, Object> body = ssafyApiClient.buildBody(header,
                "cardUniqueNo", request.getCardUniqueNo(),
                "withdrawalAccountNo", request.getWithdrawalAccountNo(),
                "withdrawalDate", request.getWithdrawalDate()
        );

        Map<String, Object> response = ssafyApiClient.post(API_PATH, body);

        @SuppressWarnings("unchecked")
        Map<String, Object> rec = (Map<String, Object>) response.get("REC");
        if (rec == null) {
            throw new NotFoundException("SSAFY API 응답에 REC이 없습니다.");
        }

        // ── 2. 공통 필드 추출 ──
        String cardNo         = (String) rec.getOrDefault("cardNo", null);
        String cvc            = (String) rec.getOrDefault("cvc", null);
        String cardUniqueNo   = (String) rec.getOrDefault("cardUniqueNo", null);
        String cardIssuerCode = (String) rec.getOrDefault("cardIssuerCode", null);
        String cardIssuerName = (String) rec.getOrDefault("cardIssuerName", null);
        String cardName       = (String) rec.getOrDefault("cardName", null);
        String cardExpiryDate = (String) rec.getOrDefault("cardExpiryDate", null);
        String cardTypeCode   = request.getCardTypeCode();
        String withdrawalAccountNo = (String) rec.getOrDefault("withdrawalAccountNo", null);
        String withdrawalDate      = (String) rec.getOrDefault("withdrawalDate", null);

        if (cardNo == null || cvc == null) {
            throw new NotFoundException("SSAFY API 응답에 필수 카드 정보가 없습니다.");
        }

        // ── 3. 중복 체크 ──
        if (creditCardRepository.existsByCardNo(cardNo) || debitCardRepository.existsByCardNo(cardNo)) {
            throw new DuplicateException("이미 등록된 카드입니다: " + cardNo);
        }

        // ── 4. cardTypeCode 분기 저장 + PayMethod 생성 ──
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
     * 신용카드 저장 + PayMethod 생성
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

        // PayMethod 자동 생성
        PayMethod paymentMethod = PayMethod.builder()
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
     * 체크카드 저장 + PayMethod 생성
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

        // PayMethod 자동 생성
        PayMethod paymentMethod = PayMethod.builder()
                .userNo(userNo)
                .methodType(MethodType.DEBIT_CARD)
                .debitCardId(debitCard.getDebitCardId())
                .build();

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("[CardService] 결제수단 생성: paymentMethodId={}", paymentMethod.getPaymentMethodId());

        return CardRegisterResponse.fromDebitCard(
                debitCard, withdrawalAccountNo, withdrawalDate, paymentMethod.getPaymentMethodId());
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getMyCards(Long userNo) {
        List<CardResponse> cards = new ArrayList<>();

        creditCardRepository.findByUserNoAndIsActiveTrue(userNo)
                .stream()
                .map(CardResponse::fromCreditCard)
                .forEach(cards::add);

        debitCardRepository.findByUserNoAndIsActiveTrue(userNo)
                .stream()
                .map(CardResponse::fromDebitCard)
                .forEach(cards::add);

        return cards;
    }

    @Transactional
    public void deleteCard(Long userNo, Long cardId, String cardType) {
        if ("CREDIT".equals(cardType)) {
            // 1. 카드 조회 + 소유자 검증
            CreditCard creditCard = creditCardRepository.findById(cardId)
                    .filter(card -> card.getUserNo().equals(userNo))
                    .orElseThrow(() -> new NotFoundException("카드를 찾을 수 없습니다: " + cardId));

            creditCard.deactivate();

            List<PayMethod> methods = paymentMethodRepository.findAllByCreditCardIdAndIsActiveTrue(cardId);
            methods.forEach(PayMethod::deactivate);

            log.info("[CardService] 신용카드 삭제: userNo={}, cardId={}", userNo, cardId);

        } else if ("DEBIT".equals(cardType)) {
            DebitCard debitCard = debitCardRepository.findById(cardId)
                    .filter(card -> card.getUserNo().equals(userNo))
                    .orElseThrow(() -> new NotFoundException("카드를 찾을 수 없습니다: " + cardId));

            debitCard.deactivate();

            List<PayMethod> methods = paymentMethodRepository.findAllByDebitCardIdAndIsActiveTrue(cardId);
            methods.forEach(PayMethod::deactivate);

            log.info("[CardService] 체크카드 삭제: userNo={}, cardId={}", userNo, cardId);

        } else {
            throw new IllegalArgumentException("잘못된 카드 타입입니다: " + cardType);
        }
    }

    @Transactional
    public List<CardTransactionResponse> getCardTransactions(Long userNo,
                                                             Long cardId, CardTransactionRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));
        CardInfo cardInfo = findCardByIdAndUserNo(cardId, userNo);
        return fetchCardTransactions(user, cardInfo, request);
    }

    @Transactional
    public List<CardTransactionResponse> getCardTransactionsByCredentials(
            Long userNo,
            String cardNo,
            String cvc,
            Long accountId,
            CardTransactionRequest request
    ) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));
        return fetchCardTransactions(user, new CardInfo(cardNo, cvc, accountId), request);
    }

    private List<CardTransactionResponse> fetchCardTransactions(
            User user,
            CardInfo cardInfo,
            CardTransactionRequest request
    ) {
        // SSAFY API 호출
        Map<String, Object> header = ssafyHeaderFactory.create(CARD_TX_API, user.getUserKey());
        Map<String, Object> body = ssafyApiClient.buildBody(header,
                "cardNo", cardInfo.cardNo(),
                "cvc", cardInfo.cvc(),
                "startDate", request.getStartDate(),
                "endDate", request.getEndDate()
        );

        Map<String, Object> response = ssafyApiClient.post(CARD_TX_PATH, body);

        Object recRaw = response.get("REC");
        if (recRaw == null || !(recRaw instanceof Map)) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rec = (Map<String, Object>) recRaw;

        Object txListRaw = rec.get("transactionList");
        if (txListRaw == null || !(txListRaw instanceof List)) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transactionList = (List<Map<String, Object>>) txListRaw;

        if (transactionList == null || transactionList.isEmpty()) {
            return List.of();
        }

        List<ParsedCardTransaction> parsedTransactions = transactionList.stream()
                .map(this::parseCardTransaction)
                .toList();

        // 각 거래를 transaction_log에 캐싱 (중복 스킵) - 배치 조회로 N+1 방지
        List<String> allTxUniqueNos = parsedTransactions.stream()
                .map(ParsedCardTransaction::ssafyTransactionId)
                .filter(id -> id != null)
                .toList();

        Map<String, TransactionLog> existingLogMap = allTxUniqueNos.isEmpty()
                ? Map.of()
                : transactionLogRepository.findBySsafyTransactionIdIn(allTxUniqueNos).stream()
                .collect(Collectors.toMap(TransactionLog::getSsafyTransactionId, Function.identity()));

        Map<String, AiConsumptionCategoryItem> classificationTargets = new LinkedHashMap<>();
        for (ParsedCardTransaction parsedTransaction : parsedTransactions) {
            TransactionLog existing = parsedTransaction.ssafyTransactionId() == null
                    ? null
                    : existingLogMap.get(parsedTransaction.ssafyTransactionId());
            if (existing != null && hasText(existing.getAiCategory())) {
                continue;
            }
            classificationTargets.put(
                    parsedTransaction.classificationId(),
                    new AiConsumptionCategoryItem(
                            parsedTransaction.classificationId(),
                            parsedTransaction.merchantName(),
                            parsedTransaction.rawCategory(),
                            parsedTransaction.cardStatus(),
                            parsedTransaction.amount(),
                            parsedTransaction.transacted()
                    )
            );
        }

        Map<String, String> aiCategories = consumptionCategoryAiClient.classifyTransactions(
                new ArrayList<>(classificationTargets.values())
        );
        List<TransactionLog> savedLogs = new ArrayList<>();

        for (ParsedCardTransaction parsedTransaction : parsedTransactions) {
            String txUniqueNo = parsedTransaction.ssafyTransactionId();

            TransactionLog existing = existingLogMap.get(txUniqueNo);
            if (existing != null) {
                if (!hasText(existing.getAiCategory())) {
                    existing.updateAiCategory(resolveAiCategory(parsedTransaction, aiCategories));
                    existing = transactionLogRepository.save(existing);
                }
                savedLogs.add(existing);
                continue;
            }

            TransactionLog logEntity = TransactionLog.builder()
                    .accountId(cardInfo.accountId())
                    .transactionType(TransactionType.WITHDRAW)
                    .amount(parsedTransaction.amount())
                    .balanceAfter(0L)
                    .counterpart(parsedTransaction.merchantName())
                    .category(parsedTransaction.rawCategory())
                    .aiCategory(resolveAiCategory(parsedTransaction, aiCategories))
                    .memo(parsedTransaction.cardStatus())
                    .ssafyTransactionId(txUniqueNo)
                    .transacted(parsedTransaction.transacted())
                    .build();

            savedLogs.add(transactionLogRepository.save(logEntity));
        }

        log.info("[CardService] 카드 결제 내역 조회: userNo={}, cardNo={}, 건수={}", user.getUserNo(), cardInfo.cardNo(), savedLogs.size());

        return savedLogs.stream()
                .map(CardTransactionResponse::from)
                .toList();
    }


    // ════════════════════════════════════════════════
    //  내부 헬퍼
    // ════════════════════════════════════════════════

    /**
     * cardId로 credit_card → debit_card 순서로 탐색 + 소유자 검증.
     * 카드 정보(cardNo, cvc, accountId)를 담은 CardInfo를 반환한다.
     */

    private CardInfo findCardByIdAndUserNo(Long cardId, Long userNo) {
        // credit_card에서 먼저 탐색
        return creditCardRepository.findById(cardId)
                .filter(card -> card.getUserNo().equals(userNo) && Boolean.TRUE.equals(card.getIsActive()))
                .map(card -> new CardInfo(card.getCardNo(), card.getCvc(), card.getAccountId()))
                .orElseGet(() ->
                        // credit에 없으면 debit_card에서 탐색
                        debitCardRepository.findById(cardId)
                                .filter(card -> card.getUserNo().equals(userNo) && Boolean.TRUE.equals(card.getIsActive()))
                                .map(card -> new CardInfo(card.getCardNo(), card.getCvc(), card.getAccountId()))
                                .orElseThrow(() -> new NotFoundException("카드를 찾을 수 없습니다: " + cardId))
                );
    }

    /**
     * "20240418" + "094431" → LocalDateTime 변환
     */
    private LocalDateTime parseTransactionDateTime(String date, String time) {
        try {
            if (date == null || date.isBlank()) {
                log.warn("[CardService] 거래 날짜가 없습니다. 현재 시각으로 대체합니다.");
                return LocalDateTime.now();
            }
            if (time == null || time.isBlank()) {
                time = "000000";
            }
            String dateTimeStr = date + time;
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            log.warn("[CardService] 거래 일시 파싱 실패: date={}, time={}", date, time);
            return LocalDateTime.now();
        }
    }

    private ParsedCardTransaction parseCardTransaction(Map<String, Object> tx) {
        String txUniqueNo = (String) tx.get("transactionUniqueNo");
        String rawCategory = (String) tx.getOrDefault("categoryName", "");
        String merchantName = (String) tx.getOrDefault("merchantName", "");
        String txDate = (String) tx.getOrDefault("transactionDate", "");
        String txTime = (String) tx.getOrDefault("transactionTime", "");
        String cardStatus = (String) tx.getOrDefault("cardStatus", "");
        Long txAmount = parseLongOrDefault(tx.get("transactionBalance"), 0L);
        LocalDateTime transacted = parseTransactionDateTime(txDate, txTime);

        return new ParsedCardTransaction(
                txUniqueNo,
                buildClassificationId(txUniqueNo, merchantName, txDate, txTime, txAmount),
                rawCategory,
                merchantName,
                cardStatus,
                txAmount,
                transacted
        );
    }

    private String resolveAiCategory(ParsedCardTransaction parsedTransaction, Map<String, String> aiCategories) {
        String aiCategory = aiCategories.get(parsedTransaction.classificationId());
        if (hasText(aiCategory)) {
            return aiCategory;
        }
        return ConsumptionCategoryFallbackMapper.mapToServiceCategory(
                parsedTransaction.rawCategory(),
                parsedTransaction.merchantName(),
                parsedTransaction.cardStatus()
        );
    }

    private String buildClassificationId(
            String txUniqueNo,
            String merchantName,
            String txDate,
            String txTime,
            Long txAmount
    ) {
        if (hasText(txUniqueNo)) {
            return txUniqueNo;
        }
        return String.join(
                "|",
                merchantName == null ? "" : merchantName,
                txDate == null ? "" : txDate,
                txTime == null ? "" : txTime,
                String.valueOf(txAmount == null ? 0L : txAmount)
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 카드 조회 결과를 담는 내부 레코드.
     */
    private record CardInfo(String cardNo, String cvc, Long accountId) {}

    private record ParsedCardTransaction(
            String ssafyTransactionId,
            String classificationId,
            String rawCategory,
            String merchantName,
            String cardStatus,
            Long amount,
            LocalDateTime transacted
    ) {}

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
