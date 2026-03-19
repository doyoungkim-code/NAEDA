package com.ssafy.naeda.domain.pay.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.dto.response.FdsEvaluationResult;
import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.service.FdsRuleService;
import com.ssafy.naeda.domain.pay.entity.MethodType;
import com.ssafy.naeda.domain.pay.entity.PayMethod;
import com.ssafy.naeda.domain.pay.entity.PayRequestStatus;
import com.ssafy.naeda.domain.pay.entity.PayStatus;
import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import com.ssafy.naeda.domain.pay.event.PayEvent;
import com.ssafy.naeda.domain.pay.event.PayEventPublisher;
import com.ssafy.naeda.domain.pay.lock.PayDistributedLock;
import com.ssafy.naeda.domain.pay.lock.PayRateLimiter;
import com.ssafy.naeda.domain.pay.repository.PayMethodRepository;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    private final FdsRuleService fdsRuleService;
    private final PayMethodRepository payMethodRepository;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final TransactionLogRepository transactionLogRepository;
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
    // 페이스페이 통합 결제 — POST /api/pay-requests/{id}/process
    // POS 단말기 → 결제 요청 생성 → 얼굴 인식 → 등록된 결제수단 자동 분기
    //   - 신용카드/체크카드 → SSAFY 카드 결제 API
    //   - 계좌 → SSAFY 계좌이체 API
    // ============================================================

    @SuppressWarnings("unchecked")
    public PayTransaction processFacePayment(Long requestId,
                                             String userId, String idempotencyKey, String pin) {

        // 1. 멱등성 체크
        if (payDbService.existsByIdempotencyKey(idempotencyKey)) {
            log.info("[Pay] 중복 요청 감지: idempotencyKey={}", idempotencyKey);
            return payDbService.findByIdempotencyKey(idempotencyKey).orElseThrow();
        }

        // 2. 분산 락 획득
        String lockOwner = distributedLock.tryAcquire("request:" + requestId);
        if (lockOwner == null) {
            throw new BadRequestException("이미 처리 중인 요청입니다.");
        }

        try {
            // 3. Redis에서 요청 정보 조회
            Map<Object, Object> requestData = payRequestRedisService.getRequest(requestId);
            if (requestData == null) {
                throw new NotFoundException("결제 요청이 만료되었습니다.");
            }

            Long storeId = Long.parseLong((String) requestData.get("storeId"));
            Long amount = Long.parseLong((String) requestData.get("amount"));

            // 4. Redis 상태 전이: PENDING → PROCESSING
            boolean transitioned = payRequestRedisService.transition(requestId, PayRequestStatus.PROCESSING);
            if (!transitioned) {
                throw new BadRequestException("처리할 수 없는 상태의 요청입니다.");
            }

            // 5. 매장 조회
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));

            // 6. User 조회 (얼굴 인식은 별도 API에서 이미 완료됨)
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

            // Rate Limit
            if (!rateLimiter.isAllowed(user.getUserNo())) {
                throw new BadRequestException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }

            // 8. FacePay 등록된 결제수단 자동 조회
            PayMethod paymentMethod = payMethodRepository
                    .findByUserNoAndIsFacePayTrueAndIsActiveTrue(user.getUserNo())
                    .orElseThrow(() -> new NotFoundException("페이스페이 결제 수단이 등록되지 않았습니다."));

            // 9. PIN 2차 인증 (pin이 전달된 경우 검증)
            boolean pinVerified = false;
            if (pin != null && !pin.isBlank()) {
                if (user.getPinPassword() == null) {
                    throw new BadRequestException("PIN이 설정되지 않았습니다.");
                }
                if (!passwordEncoder.matches(pin, user.getPinPassword())) {
                    throw new BadRequestException("PIN이 일치하지 않습니다.");
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

            // 11. 트랜잭션 엔티티 생성
            PayTransaction transaction = PayTransaction.builder()
                    .userNo(user.getUserNo())
                    .storeId(storeId)
                    .paymentMethodId(paymentMethod.getPaymentMethodId())
                    .amount(amount)
                    .idempotencyKey(idempotencyKey)
                    .status(PayStatus.FAILED)
                    .authMethod(paymentMethod.getMethodType().name())
                    .authLevel("FACE_PAY")
                    .livenessPass(true)
                    .pinVerified(pinVerified)
                    .fdsScore(fdsResult.getAnomalyScore())
                    .fdsAction(fdsResult.getAction().name())
                    .build();

            // 12. FDS BLOCK/PAUSE
            FdsAction fdsAction = fdsResult.getAction();
            if (fdsAction == FdsAction.BLOCK) {
                transaction.markBlocked("FDS 이상거래 탐지: BLOCK");
                payDbService.save(transaction);
                fdsRuleService.saveLog(transaction.getId(), user.getUserNo(), fdsResult);
                payRequestRedisService.transition(requestId, PayRequestStatus.BLOCKED);
                publishEvent(transaction);
                return transaction;
            }
            if (fdsAction == FdsAction.PAUSE) {
                transaction.markPaused("FDS 이상거래 탐지: PAUSE");
                payDbService.save(transaction);
                fdsRuleService.saveLog(transaction.getId(), user.getUserNo(), fdsResult);
                payRequestRedisService.transition(requestId, PayRequestStatus.PAUSED);
                publishEvent(transaction);
                return transaction;
            }

            // ============================================================
            // 13. ★ 결제수단 타입에 따라 자동 분기
            // ============================================================
            String ssafyTransactionId;
            MethodType methodType = paymentMethod.getMethodType();

            if (methodType == MethodType.CREDIT_CARD || methodType == MethodType.DEBIT_CARD) {
                // ── 카드 결제 (신용카드 / 체크카드) ──
                ssafyTransactionId = executeCardPayment(user, store, paymentMethod, amount, transaction, requestId);
            } else if (methodType == MethodType.ACCOUNT) {
                // ── 계좌이체 ──
                ssafyTransactionId = executeAccountPayment(user, store, paymentMethod, amount, transaction, requestId);
            } else {
                failRequest(requestId, "지원하지 않는 결제수단 타입입니다.");
                throw new BadRequestException("지원하지 않는 결제수단 타입입니다: " + methodType);
            }

            // 14. 포인트 계산
            long earnedPoints = (long) (amount * pointRate);

            // 15. 성공 처리 + DB 저장
            transaction.markSuccess(ssafyTransactionId, earnedPoints);
            transaction = payDbService.save(transaction);
            fdsRuleService.saveLog(transaction.getId(), user.getUserNo(), fdsResult);

            // 16. Redis 상태 갱신 (DB 커밋 후)
            final PayTransaction finalTransaction = transaction;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        updateRedisSuccess(requestId, finalTransaction.getId(), ssafyTransactionId);
                    }
                });
            } else {
                updateRedisSuccess(requestId, finalTransaction.getId(), ssafyTransactionId);
            }

            // 17. Kafka 이벤트 발행
            publishEvent(transaction);

            log.info("[Pay] 페이스페이 결제 성공: requestId={}, transactionId={}, method={}, amount={}",
                    requestId, transaction.getId(), methodType, amount);
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
    // 카드 결제 실행 (신용카드 / 체크카드)
    // ============================================================

    @SuppressWarnings("unchecked")
    private String executeCardPayment(User user, Store store, PayMethod paymentMethod,
                                      Long amount, PayTransaction transaction, Long requestId) {
        // 카드 정보 조회
        String cardNo;
        String cvc;
        if (paymentMethod.getMethodType() == MethodType.CREDIT_CARD) {
            var card = creditCardRepository.findById(paymentMethod.getCreditCardId())
                    .orElseThrow(() -> new NotFoundException("신용카드 정보를 찾을 수 없습니다."));
            cardNo = card.getCardNo();
            cvc = card.getCvc();
        } else {
            var card = debitCardRepository.findById(paymentMethod.getDebitCardId())
                    .orElseThrow(() -> new NotFoundException("체크카드 정보를 찾을 수 없습니다."));
            cardNo = card.getCardNo();
            cvc = card.getCvc();
        }

        // SSAFY merchantId
        Long ssafyMerchantId = store.resolveSsafyMerchantId();
        if (ssafyMerchantId == null) {
            failRequest(requestId, "매장에 SSAFY merchantId가 없습니다.");
            throw new BadRequestException("해당 매장에 SSAFY merchantId가 연결되어 있지 않습니다.");
        }

        // SSAFY 카드 결제 API 호출
        Map<String, Object> header = ssafyHeaderFactory.create("createCreditCardTransaction", user.getUserKey());
        Map<String, Object> body = ssafyApiClient.buildBody(header,
                "cardNo", cardNo,
                "cvc", cvc,
                "merchantId", ssafyMerchantId.toString(),
                "paymentBalance", amount.toString()
        );

        Map<String, Object> ssafyResponse;
        try {
            ssafyResponse = ssafyApiClient.post(CREDIT_CARD_API, body);
        } catch (Exception e) {
            transaction.markFailed("SSAFY 카드 결제 오류: " + e.getMessage());
            payDbService.save(transaction);
            payRequestRedisService.transition(requestId, PayRequestStatus.FAILED);
            log.error("[Pay] SSAFY 카드 결제 실패: requestId={}", requestId, e);
            throw new BadRequestException("카드 결제 처리 중 오류가 발생했습니다.");
        }

        String ssafyTransactionId = extractCardTransactionId(ssafyResponse);
        log.info("[Pay] 카드 결제 API 성공: requestId={}, cardType={}", requestId, paymentMethod.getMethodType());
        return ssafyTransactionId;
    }

    // ============================================================
    // 계좌이체 실행
    // ============================================================

    @SuppressWarnings("unchecked")
    private String executeAccountPayment(User user, Store store, PayMethod paymentMethod,
                                         Long amount, PayTransaction transaction, Long requestId) {
        if (paymentMethod.getAccountId() == null) {
            failRequest(requestId, "결제 수단에 계좌가 연결되지 않았습니다.");
            throw new BadRequestException("결제 수단에 계좌가 연결되지 않았습니다.");
        }
        if (store.getAccountId() == null) {
            failRequest(requestId, "매장에 입금 계좌가 설정되지 않았습니다.");
            throw new BadRequestException("매장에 입금 계좌가 설정되지 않았습니다.");
        }

        Account withdrawalAccount = accountRepository.findById(paymentMethod.getAccountId())
                .orElseThrow(() -> new NotFoundException("출금 계좌를 찾을 수 없습니다."));
        Account depositAccount = accountRepository.findById(store.getAccountId())
                .orElseThrow(() -> new NotFoundException("매장 입금 계좌를 찾을 수 없습니다."));

        // SSAFY 계좌이체 API 호출
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
            transaction.markFailed("SSAFY 계좌이체 오류: " + e.getMessage());
            payDbService.save(transaction);
            payRequestRedisService.transition(requestId, PayRequestStatus.FAILED);
            log.error("[Pay] SSAFY 이체 실패: requestId={}", requestId, e);
            throw new BadRequestException("계좌이체 처리 중 오류가 발생했습니다.");
        }

        List<Map<String, Object>> recList = (List<Map<String, Object>>) transferResponse.get("REC");
        String ssafyTransactionId = extractTransactionNo(recList);

        // TransactionLog 저장
        long balanceAfter;
        try {
            balanceAfter = getBalanceAfter(user.getUserKey(), withdrawalAccount.getAccountNo());
        } catch (Exception e) {
            log.warn("[Pay] 잔액 조회 실패: requestId={}", requestId, e);
            balanceAfter = 0L;
        }
        transactionLogRepository.save(TransactionLog.builder()
                .accountId(withdrawalAccount.getAccountId())
                .transactionType(TransactionType.WITHDRAW)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .counterpart(depositAccount.getAccountNo())
                .memo(store.getStoreName() + " 페이스페이 결제")
                .ssafyTransactionId(ssafyTransactionId)
                .build());

        log.info("[Pay] 계좌이체 API 성공: requestId={}", requestId);
        return ssafyTransactionId;
    }

    // ============================================================
    // 조회
    // ============================================================

    public PayTransaction getPayment(Long userNo, Long paymentId) {
        PayTransaction tx = payDbService.findById(paymentId)
                .filter(t -> t.getUserNo().equals(userNo))
                .orElseThrow(() -> new NotFoundException("존재하지 않는 결제 내역입니다."));
        return tx;
    }

    public List<PayTransaction> getPayments(Long userNo, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return payDbService.findByUserNoAndPeriod(userNo, from, to);
        }
        return payDbService.findByUserNo(userNo);
    }

    // ============================================================
    // 내부 헬퍼 메서드
    // ============================================================

    private FdsEvaluationResult evaluateFds(Long userNo, Long storeId, Long amount) {
        LocalDateTime now = LocalDateTime.now();

        int recentCount = payDbService.getPayTransactionRepository()
                .countByUserNoAndStatusAndCreatedAtAfter(userNo, PayStatus.SUCCESS, now.minusMinutes(10));
        long sum30d = payDbService.getPayTransactionRepository()
                .sumAmountByUserNoAndStatusAndCreatedAtAfter(userNo, PayStatus.SUCCESS, now.minusDays(30));
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

    private void updateRedisSuccess(Long requestId, Long transactionId, String ssafyTransactionId) {
        try {
            payRequestRedisService.transition(requestId, PayRequestStatus.SUCCESS);
            payRequestRedisService.setResultData(requestId, Map.of(
                    "transactionId", String.valueOf(transactionId),
                    "ssafyTransactionId", ssafyTransactionId != null ? ssafyTransactionId : ""
            ));
        } catch (Exception e) {
            log.error("[Pay] Redis 상태 업데이트 실패 (DB 커밋은 완료됨): requestId={}", requestId, e);
        }
    }

    private void failRequest(Long requestId, String reason) {
        try {
            payRequestRedisService.transition(requestId, PayRequestStatus.FAILED);
        } catch (Exception e) {
            log.warn("[Pay] Redis 상태 업데이트 실패: requestId={}", requestId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private long getBalanceAfter(String userKey, String accountNo) {
        Map<String, Object> balanceBody = ssafyApiClient.buildBody(
                ssafyHeaderFactory.create("inquireDemandDepositAccountBalance", userKey),
                "accountNo", accountNo
        );
        Map<String, Object> balanceResponse = ssafyApiClient.post(BALANCE_API, balanceBody);
        Map<String, Object> rec = (Map<String, Object>) balanceResponse.get("REC");
        if (rec == null) return 0L;
        String balance = (String) rec.get("accountBalance");
        if (balance == null) return 0L;
        try {
            return Long.parseLong(balance);
        } catch (NumberFormatException e) {
            return 0L;
        }
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
