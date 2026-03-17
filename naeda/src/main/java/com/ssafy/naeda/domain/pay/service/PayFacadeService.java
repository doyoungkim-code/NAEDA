package com.ssafy.naeda.domain.pay.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.face.service.FaceService;
import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.dto.response.FdsEvaluationResult;
import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.service.FdsRuleService;
import com.ssafy.naeda.domain.pay.entity.PayStatus;
import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import com.ssafy.naeda.domain.pay.event.PayEvent;
import com.ssafy.naeda.domain.pay.event.PayEventPublisher;
import com.ssafy.naeda.domain.pay.lock.PayDistributedLock;
import com.ssafy.naeda.domain.pay.lock.PayRateLimiter;
import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
import com.ssafy.naeda.domain.payment.repository.PaymentRepository;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayFacadeService {

    private final PayDBService payDbService;
    private final PayRequestRedisService payRequestRedisService;
    private final PayDistributedLock distributedLock;
    private final PayRateLimiter rateLimiter;
    private final PayEventPublisher eventPublisher;

    private final FaceService faceService;
    private final FdsRuleService fdsRuleService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final CreditCardRepository creditCardRepository;
    private final DebitCardRepository debitCardRepository;
    private final PasswordEncoder passwordEncoder;


    private static final String CREDIT_CARD_API = "/edu/creditCard/createCreditCardTransaction";
    private static final String TRANSFER_API = "/edu/demandDeposit/updateDemandDepositAccountTransfer";
    private static final String BALANCE_API = "/edu/demandDeposit/inquireDemandDepositAccountBalance";

    @Value("${payment.point.rate:0.05}")
    private double pointRate;

    // ============================================================
    // 카드 결제 (동기) — POST /api/payments
    // ============================================================

    @SuppressWarnings("unchecked")
    public PayTransaction processCardPayment(Long userNo, Long storeId,
                                             Long paymentMethodId, Long amount,
                                             String idempotencyKey, MultipartFile faceImage,String pin) {

        // 1. Rate Limit
        if (!rateLimiter.isAllowed(userNo)) {
            throw new RuntimeException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        // 2. 멱등성 체크
        if (payDbService.existsByIdempotencyKey(idempotencyKey)) {
            log.info("[Pay] 중복 요청 감지: idempotencyKey={}", idempotencyKey);
            return payDbService.findByIdempotencyKey(idempotencyKey).orElseThrow();
        }

        // 3. 매장 조회
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 매장입니다."));

        // 4. 얼굴 인증
        var faceResult = faceService.search(faceImage, 1, amount);

        if (faceResult.isBlocked()) {
            throw new RuntimeException("얼굴 인증이 차단되었습니다.");
        }

        // 5. User 조회
        User user = userRepository.findByUserId(faceResult.getBestUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 5-1. 2차 인증(PIN) 필요 여부 확인
        boolean pinVerified = false;
        if (!"PASS".equals(faceResult.getNextAction())) {
            if (pin == null || pin.isBlank()) {
                // PIN 미제공 → 클라이언트에게 2차인증 요구
                PayTransaction failTx = PayTransaction.builder()
                        .userNo(user.getUserNo())
                        .storeId(storeId)
                        .paymentMethodId(paymentMethodId)
                        .amount(amount)
                        .idempotencyKey(idempotencyKey)
                        .status(PayStatus.FAILED)
                        .authMethod("CARD")
                        .authLevel(faceResult.getAuthLevel().name())
                        .faceDistance((double) (1.0f - faceResult.getSimilarity()))
                        .livenessPass(true)
                        .pinVerified(false)
                        .failureReason("REQUIRE_SECOND_FACTOR:" + faceResult.getNextAction())
                        .build();
                return payDbService.save(failTx);
            }

            // PIN 검증
            if (user.getPinPassword() == null) {
                throw new RuntimeException("PIN이 설정되지 않았습니다.");
            }
            if (!passwordEncoder.matches(pin, user.getPinPassword())) {
                throw new RuntimeException("PIN이 일치하지 않습니다.");
            }
            pinVerified = true;
        }

        // 6. 결제수단 조회
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("결제 수단을 찾을 수 없습니다."));

        // 7. FDS 평가
        FdsEvaluationResult fdsResult;
        try {
            fdsResult = evaluateFds(user.getUserNo(), storeId, amount);
        } catch (Exception e) {
            log.error("[FDS] 평가 실패, NONE으로 fallback", e);
            fdsResult = new FdsEvaluationResult(0, List.of(), FdsAction.NONE);
        }

        // 8. 엔티티 생성
        PayTransaction transaction = PayTransaction.builder()
                .userNo(user.getUserNo())
                .storeId(storeId)
                .paymentMethodId(paymentMethodId)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .status(PayStatus.FAILED)
                .authMethod("CARD")
                .authLevel(faceResult.getAuthLevel().name())
                .faceDistance((double) (1.0f - faceResult.getSimilarity()))
                .livenessPass(true)
                .pinVerified(pinVerified)
                .fdsScore(fdsResult.getAnomalyScore())
                .fdsAction(fdsResult.getAction().name())
                .build();

        // 9. FDS BLOCK/PAUSE
        FdsAction fdsAction = fdsResult.getAction();
        if (fdsAction == FdsAction.BLOCK) {
            transaction.markBlocked("FDS 이상거래 탐지: BLOCK");
            payDbService.save(transaction);
            publishEvent(transaction);
            return transaction;
        }
        if (fdsAction == FdsAction.PAUSE) {
            transaction.markPaused("FDS 이상거래 탐지: PAUSE");
            payDbService.save(transaction);
            publishEvent(transaction);
            return transaction;
        }

        // 10. 카드 정보 조회
        String cardNo;
        String cvc;
        if (paymentMethod.getMethodType() == com.ssafy.naeda.domain.payment.entity.MethodType.CREDIT_CARD) {
            var card = creditCardRepository.findById(paymentMethod.getCreditCardId())
                    .orElseThrow(() -> new RuntimeException("신용카드 정보를 찾을 수 없습니다."));
            cardNo = card.getCardNo();
            cvc = card.getCvc();
        } else if (paymentMethod.getMethodType() == com.ssafy.naeda.domain.payment.entity.MethodType.DEBIT_CARD) {
            var card = debitCardRepository.findById(paymentMethod.getDebitCardId())
                    .orElseThrow(() -> new RuntimeException("체크카드 정보를 찾을 수 없습니다."));
            cardNo = card.getCardNo();
            cvc = card.getCvc();
        } else {
            throw new RuntimeException("카드 결제 수단만 지원합니다.");
        }

        // 11. ★ SSAFY 카드 결제 API 호출 (@Transactional 밖!)
        Map<String, Object> header = ssafyHeaderFactory.create("createCreditCardTransaction", user.getUserKey());
        Map<String, Object> body = ssafyApiClient.buildBody(header,
                "cardNo", cardNo,
                "cvc", cvc,
                "merchantId", store.getStoreId().toString(),
                "paymentBalance", amount.toString()
        );

        Map<String, Object> ssafyResponse;
        try {
            ssafyResponse = ssafyApiClient.post(CREDIT_CARD_API, body);
        } catch (Exception e) {
            transaction.markFailed("SSAFY 카드 결제 오류: " + e.getMessage());
            payDbService.save(transaction);
            log.error("[Pay] SSAFY 카드 결제 실패", e);
            throw new RuntimeException("카드 결제 처리 중 오류가 발생했습니다.");
        }

        // 12. 거래 ID 추출
        String ssafyTransactionId = extractCardTransactionId(ssafyResponse);
        long earnedPoints = (long) (amount * pointRate);

        // 13. 성공 처리 + DB 저장
        transaction.markSuccess(ssafyTransactionId, earnedPoints);
        transaction = payDbService.save(transaction);

        // 14. Kafka 이벤트 발행
        publishEvent(transaction);

        log.info("[Pay] 카드 결제 성공: transactionId={}, amount={}", transaction.getId(), amount);
        return transaction;
    }

    // ============================================================
    // 계좌 결제 (비동기) — POST /api/payment-requests/{id}/process
    // ============================================================

    @SuppressWarnings("unchecked")
    public PayTransaction processAccountPayment(Long requestId,
                                                String idempotencyKey, MultipartFile faceImage,String pin) {



        // 1. Rate Limit
        // (userNo는 얼굴 인증 후 알 수 있으므로, requestId 기반으로 체크)

        // 2. 멱등성 체크
        if (payDbService.existsByIdempotencyKey(idempotencyKey)) {
            log.info("[Pay] 중복 요청 감지: idempotencyKey={}", idempotencyKey);
            return payDbService.findByIdempotencyKey(idempotencyKey).orElseThrow();
        }

        // 3. 분산 락 획득
        String lockOwner = distributedLock.tryAcquire("request:" + requestId);
        if (lockOwner == null) {
            throw new RuntimeException("이미 처리 중인 요청입니다.");
        }

        try {
            // 4. Redis에서 요청 정보 조회
            Map<Object, Object> requestData = payRequestRedisService.getRequest(requestId);
            if (requestData == null) {
                throw new RuntimeException("결제 요청이 만료되었습니다.");
            }

            Long storeId = Long.parseLong((String) requestData.get("storeId"));
            Long amount = Long.parseLong((String) requestData.get("amount"));
            Long paymentMethodId = Long.parseLong((String) requestData.get("paymentMethodId"));

            // 5. Redis 상태 전이: PENDING → PROCESSING
            boolean transitioned = payRequestRedisService.transition(
                    requestId, com.ssafy.naeda.domain.pay.entity.PayRequestStatus.PROCESSING);
            if (!transitioned) {
                throw new RuntimeException("처리할 수 없는 상태의 요청입니다.");
            }

            // 6. 매장 조회
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 매장입니다."));

            // 7. 얼굴 인증
            var faceResult = faceService.search(faceImage, 1, amount);

            if (faceResult.isBlocked()) {
                payRequestRedisService.transition(requestId,
                        com.ssafy.naeda.domain.pay.entity.PayRequestStatus.BLOCKED);
                throw new RuntimeException("얼굴 인증이 차단되었습니다.");
            }

            // 8. User 조회
            User user = userRepository.findByUserId(faceResult.getBestUserId())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));



            // Rate Limit (userNo 확인 후)
            if (!rateLimiter.isAllowed(user.getUserNo())) {
                throw new RuntimeException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }

            // 9. 결제수단 조회
            PaymentMethod paymentMethod = paymentMethodRepository
                    .findByUserNoAndIsFacePayTrueAndIsActiveTrue(user.getUserNo())
                    .orElseThrow(() -> new RuntimeException("페이스페이 결제 수단이 등록되지 않았습니다."));

            // 8-1. 2차 인증 (PIN)
            boolean pinVerified = false;
            if (!"PASS".equals(faceResult.getNextAction())) {
                if (pin == null || pin.isBlank()) {
                    payRequestRedisService.transition(requestId,
                            com.ssafy.naeda.domain.pay.entity.PayRequestStatus.FAILED);
                    PayTransaction failTx = PayTransaction.builder()
                            .userNo(user.getUserNo())
                            .storeId(storeId)
                            .paymentMethodId(paymentMethod.getPaymentMethodId())
                            .amount(amount)
                            .idempotencyKey(idempotencyKey)
                            .status(PayStatus.FAILED)
                            .authMethod("ACCOUNT")
                            .authLevel(faceResult.getAuthLevel().name())
                            .faceDistance((double) (1.0f - faceResult.getSimilarity()))
                            .livenessPass(true)
                            .pinVerified(false)
                            .failureReason("REQUIRE_SECOND_FACTOR:" + faceResult.getNextAction())
                            .build();
                    return payDbService.save(failTx);
                }

                if (user.getPinPassword() == null) {
                    throw new RuntimeException("PIN이 설정되지 않았습니다.");
                }
                if (!passwordEncoder.matches(pin, user.getPinPassword())) {
                    throw new RuntimeException("PIN이 일치하지 않습니다.");
                }
                pinVerified = true;
            }

            // 10. FDS 평가
            FdsEvaluationResult fdsResult;
            try {
                fdsResult = evaluateFds(user.getUserNo(), storeId, amount);
            } catch (Exception e) {
                log.error("[FDS] 평가 실패, NONE으로 fallback", e);
                fdsResult = new FdsEvaluationResult(0, List.of(), FdsAction.NONE);
            }

            // 11. 엔티티 생성
            PayTransaction transaction = PayTransaction.builder()
                    .userNo(user.getUserNo())
                    .storeId(storeId)
                    .paymentMethodId(paymentMethod.getPaymentMethodId())
                    .amount(amount)
                    .idempotencyKey(idempotencyKey)
                    .status(PayStatus.FAILED)
                    .authMethod("ACCOUNT")
                    .authLevel(faceResult.getAuthLevel().name())
                    .faceDistance((double) (1.0f - faceResult.getSimilarity()))
                    .livenessPass(true)
                    .pinVerified(pinVerified)
                    .fdsScore(fdsResult.getAnomalyScore())
                    .fdsAction(fdsResult.getAction().name())
                    .build();

            // 12. FDS BLOCK/PAUSE 처리
            FdsAction fdsAction = fdsResult.getAction();
            if (fdsAction == FdsAction.BLOCK) {
                transaction.markBlocked("FDS 이상거래 탐지: BLOCK");
                payDbService.save(transaction);
                payRequestRedisService.transition(requestId,
                        com.ssafy.naeda.domain.pay.entity.PayRequestStatus.BLOCKED);
                publishEvent(transaction);
                return transaction;
            }
            if (fdsAction == FdsAction.PAUSE) {
                transaction.markPaused("FDS 이상거래 탐지: PAUSE");
                payDbService.save(transaction);
                payRequestRedisService.transition(requestId,
                        com.ssafy.naeda.domain.pay.entity.PayRequestStatus.PAUSED);
                publishEvent(transaction);
                return transaction;
            }

            // 13. 출금/입금 계좌 조회
            Account withdrawalAccount = accountRepository.findById(paymentMethod.getAccountId())
                    .orElseThrow(() -> new RuntimeException("출금 계좌를 찾을 수 없습니다."));
            Account depositAccount = accountRepository.findById(store.getAccountId())
                    .orElseThrow(() -> new RuntimeException("매장 입금 계좌를 찾을 수 없습니다."));

            // 14. ★ SSAFY 계좌이체 API 호출 (@Transactional 밖!)
            Map<String, Object> transferBody = ssafyApiClient.buildBody(
                    ssafyHeaderFactory.create("updateDemandDepositAccountTransfer", user.getUserKey()),
                    "depositAccountNo", depositAccount.getAccountNo(),
                    "depositTransactionSummary", store.getStoreName() + " 결제",
                    "transactionBalance", amount.toString(),
                    "withdrawalAccountNo", withdrawalAccount.getAccountNo(),
                    "withdrawalTransactionSummary", store.getStoreName() + " 페이스페이 결제"
            );

            Map<String, Object> transferResponse;
            try {
                transferResponse = ssafyApiClient.post(TRANSFER_API, transferBody);
            } catch (Exception e) {
                transaction.markFailed("SSAFY API 오류: " + e.getMessage());
                payDbService.save(transaction);
                payRequestRedisService.transition(requestId,
                        com.ssafy.naeda.domain.pay.entity.PayRequestStatus.FAILED);
                log.error("[Pay] SSAFY 이체 실패: requestId={}", requestId, e);
                throw new RuntimeException("결제 처리 중 오류가 발생했습니다.");
            }

            // 15. 거래 ID 추출
            List<Map<String, Object>> recList = (List<Map<String, Object>>) transferResponse.get("REC");
            String ssafyTransactionId = extractTransactionNo(recList);

            // 16. 포인트 계산
            int earnedPoints = (int) (amount * pointRate);

            // 17. 성공 처리 + DB 저장
            transaction.markSuccess(ssafyTransactionId, (long) earnedPoints);
            transaction = payDbService.save(transaction);

            // 18. Redis 상태: PROCESSING → SUCCESS
            payRequestRedisService.transition(requestId,
                    com.ssafy.naeda.domain.pay.entity.PayRequestStatus.SUCCESS);
            payRequestRedisService.setResultData(requestId, Map.of(
                    "transactionId", String.valueOf(transaction.getId()),
                    "ssafyTransactionId", ssafyTransactionId != null ? ssafyTransactionId : ""
            ));

            // 19. Kafka 이벤트 발행 (포인트/FDS/알림 비동기 처리)
            publishEvent(transaction);

            log.info("[Pay] 계좌 결제 성공: requestId={}, transactionId={}, amount={}",
                    requestId, transaction.getId(), amount);
            return transaction;

        } catch (Exception e) {
            log.error("[Pay] 결제 실패: requestId={}", requestId, e);
            throw e;
        } finally {
            // ★ 반드시 락 해제
            distributedLock.release("request:" + requestId, lockOwner);
        }
    }

    // ============================================================
    // 조회
    // ============================================================

    public PayTransaction getPayment(Long paymentId) {
        return payDbService.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제 내역을 찾을 수 없습니다."));
    }

    public List<PayTransaction> getPayments(Long userNo) {
        return payDbService.findByUserNo(userNo);
    }

    // ============================================================
    // 내부 메서드
    // ============================================================

    private FdsEvaluationResult evaluateFds(Long userNo, Long storeId, Long amount) {
        LocalDateTime now = LocalDateTime.now();

        int recentCount = paymentRepository.countByUserNoAndStatusAndPaidAfter(
                userNo, com.ssafy.naeda.domain.payment.entity.PaymentStatus.SUCCESS, now.minusMinutes(10));
        long sum30d = paymentRepository.sumAmountByUserNoAndStatusAndPaidAfter(
                userNo, com.ssafy.naeda.domain.payment.entity.PaymentStatus.SUCCESS, now.minusDays(30));
        long dailyAvg = sum30d / 30;

        FdsEvaluationRequest request = FdsEvaluationRequest.builder()
                .userNo(userNo)
                .storeId(storeId)
                .amount(amount)
                .paymentTime(now)
                .recentPaymentCount(recentCount)
                .dailyAverageAmount(dailyAvg)
                .build();

        return fdsRuleService.evaluate(request);
    }

    private String extractTransactionNo(List<Map<String, Object>> recList) {
        if (recList == null || recList.isEmpty()) return null;
        return recList.stream()
                .filter(rec -> "2".equals(rec.get("transactionType")))
                .map(rec -> (String) rec.get("transactionUniqueNo"))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private String extractCardTransactionId(Map<String, Object> response) {
        Object rec = response.get("REC");
        if (rec instanceof Map) {
            return (String) ((Map<String, Object>) rec).get("transactionUniqueNo");
        }
        return null;
    }


    private void publishEvent(PayTransaction tx) {
        PayEvent event = PayEvent.builder()
                .transactionId(tx.getId())
                .userNo(tx.getUserNo())
                .storeId(tx.getStoreId())
                .amount(tx.getAmount())
                .status(tx.getStatus().name())
                .authMethod(tx.getAuthMethod())
                .fdsScore(tx.getFdsScore())
                .fdsAction(tx.getFdsAction())
                .earnedPoints(tx.getEarnedPoints())
                .ssafyTransactionId(tx.getSsafyTransactionId())
                .build();

        eventPublisher.publish(event);
    }
}
