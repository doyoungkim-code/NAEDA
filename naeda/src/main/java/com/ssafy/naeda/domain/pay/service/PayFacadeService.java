package com.ssafy.naeda.domain.pay.service;

import com.ssafy.naeda.domain.account.entity.Account;
import com.ssafy.naeda.domain.account.repository.AccountRepository;
import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
import com.ssafy.naeda.domain.consumption.client.ConsumptionMonthlyInsightAiClient;
import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.dto.response.FdsEvaluationResult;
import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.service.FdsRuleService;
import com.ssafy.naeda.domain.pay.dto.response.CurrentMonthSpendingAnalysisResponse;
import com.ssafy.naeda.domain.pay.dto.response.PayTransactionResponse;
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
import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.service.PointService;
import com.ssafy.naeda.domain.rba.dto.AuthMethod;
import com.ssafy.naeda.domain.rba.service.PhoneVerificationService;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
import com.ssafy.naeda.domain.transaction.entity.TransactionType;
import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.entity.ReferenceType;
import com.ssafy.naeda.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayFacadeService {

    private final PayDBService payDbService;
    private final PayRequestRedisService payRequestRedisService;
    private final PayDistributedLock distributedLock;
    private final PayRateLimiter rateLimiter;
    private final FdsRuleService fdsRuleService;
    private final PayMethodRepository payMethodRepository;
    private final PointService pointService;
    private final AccountRepository accountRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final CreditCardRepository creditCardRepository;
    private final DebitCardRepository debitCardRepository;
    private final PasswordEncoder passwordEncoder;
    private final FcmService fcmService;
    private final PayLimitService payLimitService;
    private final ConsumptionMonthlyInsightAiClient consumptionMonthlyInsightAiClient;
    private final PhoneVerificationService phoneVerificationService;
    private final PayEventPublisher eventPublisher;



    private static final String CREDIT_CARD_API = "/edu/creditCard/createCreditCardTransaction";
    private static final String TRANSFER_API = "/edu/demandDeposit/updateDemandDepositAccountTransfer";
    private static final String BALANCE_API = "/edu/demandDeposit/inquireDemandDepositAccountBalance";

    @Value("${payment.point.rate:0.05}")
    private double pointRate;

    @Value("${rba.high-amount:50000}")
    private long highAmountThreshold;

    // ============================================================
    // 페이스페이 통합 결제 — POST /api/pay-requests/{id}/process
    // POS 단말기 → 결제 요청 생성 → 얼굴 인식 → 등록된 결제수단 자동 분기
    //   - 신용카드/체크카드 → SSAFY 카드 결제 API
    //   - 계좌 → SSAFY 계좌이체 API
    // ============================================================

    @SuppressWarnings("unchecked")
    public PayTransaction processFacePayment(Long requestId,
                                             Long userNo,
                                             String idempotencyKey,
                                             String pin,
                                             String phoneMiddleDigits,
                                             FaceMatchStatus faceStatus,
                                             AuthMethod selectedAuthMethod,
                                             Boolean signatureConfirmed) {

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
            User user = userRepository.findById(userNo)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

            // Rate Limit
            if (!rateLimiter.isAllowed(user.getUserNo())) {
                throw new BadRequestException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }

            // 8. FacePay 등록된 결제수단 자동 조회
            List<PayMethod> facePayMethods = payMethodRepository
                    .findByUserNoAndIsFacePayTrueAndIsActiveTrue(user.getUserNo());
            if (facePayMethods.isEmpty()) {
                throw new NotFoundException("페이스페이 결제 수단이 등록되지 않았습니다.");
            }

            //Facepay 결제 수단 선택
            //유저가 FacePay로 등록한 결제 수단이 여러 개일 수 있으므로,
            //기본 결제수단(isDefault = true)을 우선 선택한다.
            //기본 결제수단이 없으면 가장 먼저 등록된 결제 수단을 사용한다.

            PayMethod paymentMethod = facePayMethods.stream()
                    .filter(PayMethod::getIsDefault)
                    .findFirst()
                    .orElse(facePayMethods.get(0));

            //결제 한도 검증
            //결제 요청 금액이 유저가 설정한 1회/1일/월 한도를 초과하는지 검증한다.
            // SUCCESS 상태의 거래만 누적 합산하여 비교한다.
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

            long todaySum = payDbService.getPayTransactionRepository()
                    .sumAmountByUserNoAndStatusAndCreatedAtAfter(
                            user.getUserNo(), PayStatus.SUCCESS, todayStart
                    );
            long monthSum = payDbService.getPayTransactionRepository()
                    .sumAmountByUserNoAndStatusAndCreatedAtAfter(
                            user.getUserNo(),PayStatus.SUCCESS,monthStart
                    );

            payLimitService.validatePaymentLimit(user.getUserNo(),amount,todaySum,monthSum);

            FaceAuthValidation authValidation = validateFaceAuth(
                    user,
                    amount,
                    faceStatus,
                    selectedAuthMethod,
                    pin,
                    phoneMiddleDigits,
                    signatureConfirmed
            );
            boolean pinVerified = authValidation.pinVerified();

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
                    .authLevel(authValidation.authLevel())
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
                return transaction;
            }
            if (fdsAction == FdsAction.PAUSE) {
                transaction.markPaused("FDS 이상거래 탐지: PAUSE");
                payDbService.save(transaction);
                fdsRuleService.saveLog(transaction.getId(), user.getUserNo(), fdsResult);
                payRequestRedisService.transition(requestId, PayRequestStatus.PAUSED);
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

            // 16. 포인트 적립은 Kafka Consumer(pay-point-group)에서 처리
            // accumulateEarnedPoints() 직접 호출 제거 — Consumer와 중복 적립 방지

            // 17. Redis 상태 갱신
            updateRedisSuccess(requestId, transaction.getId(), ssafyTransactionId);

            // 18. Kafka 이벤트 발행 (포인트 적립 / FDS 알림 / 결제 알림 비동기 후처리)
            // 결제 성공 시에만 발행하며, Consumer가 각각 독립적으로 처리
            try {
                PayEvent event = PayEvent.builder()
                        .transactionId(transaction.getId())
                        .userNo(user.getUserNo())
                        .storeId(storeId)
                        .amount(amount)
                        .status(transaction.getStatus().name())
                        .authMethod(paymentMethod.getMethodType().name())
                        .fdsScore(fdsResult.getAnomalyScore())
                        .fdsAction(fdsResult.getAction().name())
                        .earnedPoints(earnedPoints)
                        .ssafyTransactionId(ssafyTransactionId)
                        .build();
                eventPublisher.publish(event);
            } catch (Exception e) {
                log.warn("[Pay] Kafka 이벤트 발행 실패 (결제는 성공): requestId={}", requestId, e);
            }

            log.info("[Pay] 페이스페이 결제 성공: requestId={}, transactionId={}, method={}, amount={}",
                    requestId, transaction.getId(), methodType, amount);
            return transaction;

        } catch (Exception e) {
            log.error("[Pay] 결제 실패: requestId={}", requestId, e);
            failRequest(requestId, e.getMessage());
            throw e;
        } finally {
            // ★ 반드시 락 해제
            distributedLock.release("request:" + requestId, lockOwner);
        }
    }

    private FaceAuthValidation validateFaceAuth(User user,
                                                Long amount,
                                                FaceMatchStatus faceStatus,
                                                AuthMethod selectedAuthMethod,
                                                String pin,
                                                String phoneMiddleDigits,
                                                Boolean signatureConfirmed) {
        if (faceStatus == null || faceStatus == FaceMatchStatus.NO_MATCH) {
            throw new BadRequestException("얼굴을 다시 인식해 주세요.");
        }

        boolean pinVerified = false;
        String authLevel = "FACE_ONLY";

        if (faceStatus == FaceMatchStatus.MATCH) {
            if (Boolean.TRUE.equals(user.getSecondaryAuthEnabled())) {
                if (selectedAuthMethod != AuthMethod.PIN) {
                    throw new BadRequestException("PIN 번호를 입력해 주세요.");
                }
                verifyPin(user, pin);
                pinVerified = true;
                authLevel = "FACE_PIN";
            }
        } else if (faceStatus == FaceMatchStatus.AMBIGUOUS) {
            if (selectedAuthMethod == AuthMethod.PIN) {
                verifyPin(user, pin);
                pinVerified = true;
                authLevel = "FACE_PIN";
            } else if (selectedAuthMethod == AuthMethod.PHONE) {
                verifyPhone(user, phoneMiddleDigits);
                authLevel = "FACE_PHONE";
            } else {
                throw new BadRequestException("PIN 번호 또는 전화번호 4자리 인증이 필요합니다.");
            }
        }

        if (amount >= highAmountThreshold) {
            if (!Boolean.TRUE.equals(signatureConfirmed)) {
                throw new BadRequestException("5만원 이상 결제는 서명이 필요합니다.");
            }
            authLevel = switch (authLevel) {
                case "FACE_PIN" -> "FACE_PIN_SIGNATURE";
                case "FACE_PHONE" -> "FACE_PHONE_SIGNATURE";
                default -> "FACE_SIGNATURE";
            };
        }

        return new FaceAuthValidation(authLevel, pinVerified);
    }

    private void verifyPin(User user, String pin) {
        if (pin == null || pin.isBlank()) {
            throw new BadRequestException("PIN 번호를 입력해 주세요.");
        }
        if (user.getPinPassword() == null) {
            throw new BadRequestException("PIN이 설정되지 않았습니다.");
        }
        if (!passwordEncoder.matches(pin, user.getPinPassword())) {
            throw new BadRequestException("PIN이 일치하지 않습니다.");
        }
    }

    private void verifyPhone(User user, String phoneMiddleDigits) {
        if (phoneMiddleDigits == null || phoneMiddleDigits.isBlank()) {
            throw new BadRequestException("전화번호 가운데 4자리를 입력해 주세요.");
        }
        if (!phoneVerificationService.verify(user.getUserNo(), phoneMiddleDigits)) {
            throw new BadRequestException("전화번호 가운데 4자리가 일치하지 않습니다.");
        }
    }

    private record FaceAuthValidation(String authLevel, boolean pinVerified) {
    }

    private void accumulateEarnedPoints(Long userNo, Long transactionId, Long earnedPoints) {
        if (earnedPoints == null || earnedPoints <= 0) {
            return;
        }

        PointEarnRequest earnRequest = PointEarnRequest.builder()
                .amount(earnedPoints)
                .description("결제 적립")
                .paymentId(transactionId)
                .build();

        try {
            pointService.earnPoints(userNo, earnRequest);
            log.info("[Pay] 포인트 적립 완료: userNo={}, transactionId={}, points={}",
                    userNo, transactionId, earnedPoints);
        } catch (NotFoundException e) {
            log.info("[Pay] 포인트 지갑이 없어 생성 후 적립 재시도: userNo={}", userNo);
            try {
                pointService.createWallet(userNo);
            } catch (DuplicateException duplicateException) {
                log.info("[Pay] 포인트 지갑이 이미 생성됨: userNo={}", userNo);
            }

            try {
                pointService.earnPoints(userNo, earnRequest);
                log.info("[Pay] 포인트 적립 재시도 성공: userNo={}, transactionId={}, points={}",
                        userNo, transactionId, earnedPoints);
            } catch (Exception retryEx) {
                log.error("[Pay] 포인트 적립 재시도 실패: transactionId={}", transactionId, retryEx);
            }
        } catch (Exception e) {
            log.error("[Pay] 포인트 적립 실패: transactionId={}", transactionId, e);
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
            //카드 소유자와 결제 요청 유저가 일치하는지 검증. 소유자 검증
            if(!card.getUserNo().equals(user.getUserNo())){
                throw new BadRequestException("본인 소유의 카드가 아닙니다.");
            }

            cardNo = card.getCardNo();
            cvc = card.getCvc();
        } else {
            var card = debitCardRepository.findById(paymentMethod.getDebitCardId())
                    .orElseThrow(() -> new NotFoundException("체크카드 정보를 찾을 수 없습니다."));
            //카드 소유자와 결제 요청 유저가 일치하는지 검증. 소유자 검증
            if(!card.getUserNo().equals(user.getUserNo())){
                throw new BadRequestException("본인 소유의 카드가 아닙니다.");
            }
            cardNo = card.getCardNo();
            cvc = card.getCvc();
        }

        //체크카드 잔액 사전 검증
        //체크카드는 연결계좌에서 즉시 출금되므로, 결제 전 잔액 확인
        //신용카드는 신용한도 사용이라 검증 불필요
        if(paymentMethod.getMethodType() == MethodType.DEBIT_CARD && paymentMethod.getAccountId() != null){
            try{
                Account debitAccount = accountRepository.findById(paymentMethod.getAccountId())
                        .orElse(null);
                if(debitAccount != null){
                    long currentBalance = getBalanceAfter(user.getUserKey(),debitAccount.getAccountNo());
                    if(currentBalance < amount){
                        transaction.markFailed("잔액 부족");
                        payDbService.save(transaction);
                        payRequestRedisService.transition(requestId,PayRequestStatus.FAILED);
                        throw new BadRequestException("잔액이 부족합니다. (현재 잔액 : "+ currentBalance + "원, 결제 금액 : " + amount + "원)");
                    }
                }
            }catch(BadRequestException e){
                throw e;
            }catch(Exception e){
                log.warn("[Pay] 체크카드 잔액 조회 실패, SSAFY API에 위임: requestId:{}", requestId, e);
            }
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

        // 계좌 잔액 사전 검증
        // SSAFY 이체 API를 호출하기 전에 출금 계좌의 잔액을 확인한다.
        // 잔액 부족 시 SSAFY API를 호출 없이 즉시 실패 처리하여
        // 불필요한 외부 API 호출을 줄이고, 에러 메시지 전달
        try{
            long currentBalance = getBalanceAfter(user.getUserKey(), withdrawalAccount.getAccountNo());
            if(currentBalance < amount){
                transaction.markFailed("잔액 부족");
                payDbService.save(transaction);
                payRequestRedisService.transition(requestId,PayRequestStatus.FAILED);
                throw new BadRequestException("잔액이 부족합니다. (현재 잔액: " + currentBalance + ")");
            }
        }catch(BadRequestException e){
            // 잔액 부족 예외는 그대로 전파
            throw e;
        }catch (Exception e){
            //잔액 조회 실패시 로그만 남기고 SSAFY API에 위임
            // 잔액 조회 장애로 결제 자체가 막히면 안됨
            log.warn("[Pay] 사전 잔액 조회 실패, SSAFY API에 위임 : requestId={}", requestId,e);
        }

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

    public PayTransactionResponse getPaymentResponse(Long userNo, Long paymentId) {
        PayTransaction payment = getPayment(userNo, paymentId);
        Store store = payment.getStoreId() == null
                ? null
                : storeRepository.findById(payment.getStoreId()).orElse(null);
        return PayTransactionResponse.from(
                payment,
                store == null ? null : store.getStoreName(),
                store == null ? null : store.getCategoryName()
        );
    }

    public List<PayTransaction> getPayments(Long userNo, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return payDbService.findByUserNoAndPeriod(userNo, from, to);
        }
        return payDbService.findByUserNo(userNo);
    }

    public List<PayTransactionResponse> getPaymentResponses(Long userNo, LocalDateTime from, LocalDateTime to) {
        List<PayTransaction> payments = getPayments(userNo, from, to);
        Map<Long, Store> storeMap = buildStoreMap(payments);
        return payments.stream()
                .map(payment -> {
                    Store store = payment.getStoreId() == null ? null : storeMap.get(payment.getStoreId());
                    return PayTransactionResponse.from(
                            payment,
                            store == null ? null : store.getStoreName(),
                            store == null ? null : store.getCategoryName()
                    );
                })
                .toList();
    }

    public CurrentMonthSpendingAnalysisResponse getCurrentMonthSpendingAnalysis(Long userNo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        List<PayTransaction> currentMonthTransactions = payDbService.findByUserNoAndPeriod(userNo, periodStart, now)
                .stream()
                .filter(transaction -> transaction.getStatus() == PayStatus.SUCCESS)
                .toList();

        Map<Long, Store> storeMap = buildStoreMap(currentMonthTransactions);
        LinkedHashMap<String, Long> categoryBreakdown = currentMonthTransactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> resolveCategoryName(transaction, storeMap),
                        LinkedHashMap::new,
                        Collectors.summingLong(transaction -> transaction.getAmount() == null ? 0L : transaction.getAmount())
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .collect(
                        LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll
                );

        long totalSpending = currentMonthTransactions.stream()
                .map(PayTransaction::getAmount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        Map.Entry<String, Long> topEntry = categoryBreakdown.entrySet().stream()
                .findFirst()
                .orElse(null);

        return CurrentMonthSpendingAnalysisResponse.builder()
                .periodStart(periodStart)
                .periodEnd(now)
                .totalSpending(totalSpending)
                .transactionCount(currentMonthTransactions.size())
                .topCategory(topEntry == null ? null : topEntry.getKey())
                .topAmount(topEntry == null ? 0L : topEntry.getValue())
                .categoryBreakdown(categoryBreakdown)
                .insights(consumptionMonthlyInsightAiClient.generateMonthlyInsights(
                        periodStart.toLocalDate(),
                        now.toLocalDate(),
                        totalSpending,
                        categoryBreakdown,
                        null,
                        null
                ))
                .build();
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

    private Map<Long, Store> buildStoreMap(List<PayTransaction> transactions) {
        Set<Long> storeIds = transactions.stream()
                .map(PayTransaction::getStoreId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        return storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(Store::getStoreId, Function.identity()));
    }

    private String resolveCategoryName(PayTransaction transaction, Map<Long, Store> storeMap) {
        if (transaction.getStoreId() == null) {
            return "기타";
        }

        Store store = storeMap.get(transaction.getStoreId());
        if (store == null || store.getCategoryName() == null || store.getCategoryName().isBlank()) {
            return "기타";
        }

        return store.getCategoryName();
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
            payRequestRedisService.markFailed(requestId, reason);
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

}
