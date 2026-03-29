//package com.ssafy.naeda.domain.payment.service;
//
//import com.ssafy.naeda.domain.account.entity.Account;
//import com.ssafy.naeda.domain.account.repository.AccountRepository;
//import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
//import com.ssafy.naeda.domain.face.service.FaceService;
//import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
//import com.ssafy.naeda.domain.fds.dto.response.FdsEvaluationResult;
//import com.ssafy.naeda.domain.fds.entity.FdsAction;
//import com.ssafy.naeda.domain.fds.service.FdsRuleService;
//import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
//import com.ssafy.naeda.domain.payment.dto.response.ProcessPaymentResponse;
//import com.ssafy.naeda.domain.payment.entity.*;
//import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
//import com.ssafy.naeda.domain.payment.repository.PaymentRepository;
//import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
//import com.ssafy.naeda.domain.point.service.PointService;
//import com.ssafy.naeda.domain.store.entity.Store;
//import com.ssafy.naeda.domain.store.repository.StoreRepository;
//import com.ssafy.naeda.domain.transaction.entity.TransactionLog;
//import com.ssafy.naeda.domain.transaction.entity.TransactionType;
//import com.ssafy.naeda.domain.transaction.repository.TransactionLogRepository;
//import com.ssafy.naeda.domain.user.entity.User;
//import com.ssafy.naeda.domain.user.repository.UserRepository;
//import com.ssafy.naeda.global.exception.BadRequestException;
//import com.ssafy.naeda.global.exception.NotFoundException;
//import com.ssafy.naeda.global.ssafy.SsafyApiClient;
//import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.transaction.support.TransactionSynchronization;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class PaymentProcessService {
//
//    private static final String TRANSFER_API = "/edu/demandDeposit/updateDemandDepositAccountTransfer";
//    private static final String BALANCE_API = "/edu/demandDeposit/inquireDemandDepositAccountBalance";
//
//    private final PaymentRequestRedisService redisService;
//    private final PaymentRepository paymentRepository;
//    private final PaymentMethodRepository paymentMethodRepository;
//    private final AccountRepository accountRepository;
//    private final StoreRepository storeRepository;
//    private final UserRepository userRepository;
//    private final TransactionLogRepository transactionLogRepository;
//    private final FaceService faceService;
//    private final FdsRuleService fdsRuleService;
//    private final PointService pointService;
//    private final SsafyApiClient ssafyApiClient;
//    private final SsafyHeaderFactory ssafyHeaderFactory;
//
//    @Value("${payment.point.rate:0.05}")
//    private double pointRate;
//
//    @SuppressWarnings("unchecked")
//    @Transactional
//    public ProcessPaymentResponse processPayment(String requestId,
//                                                  MultipartFile faceImage) {
//        // 1. Redis에서 결제 요청 조회 + PENDING 확인
//        PaymentRequestData data = redisService.getRequest(requestId);
//        if (data == null) {
//            throw new NotFoundException("결제 요청이 만료되었거나 존재하지 않습니다.");
//        }
//        if (data.getStatus() != PaymentRequestStatus.PENDING) {
//            throw new BadRequestException("이미 처리 중이거나 완료된 결제 요청입니다.");
//        }
//
//        // 2. 분산 락 획득 + PENDING → PROCESSING
//        if (!redisService.tryAcquireProcessingLock(requestId)) {
//            throw new BadRequestException("이미 처리 중이거나 완료된 결제 요청입니다.");
//        }
//        redisService.updateStatus(requestId, PaymentRequestStatus.PROCESSING);
//
//        // 3. 매장 조회
//        Store store = storeRepository.findById(data.getStoreId())
//                .orElseThrow(() -> {
//                    failRequest(requestId, "존재하지 않는 매장입니다.");
//                    return new NotFoundException("존재하지 않는 매장입니다.");
//                });
//
//        // 4. 얼굴 인증 + RBA 평가
//        SearchResponse faceResult;
//        try {
//            faceResult = faceService.search(faceImage, 1, data.getAmount());
//        } catch (Exception e) {
//            failRequest(requestId, "얼굴 인증 실패: " + e.getMessage());
//            throw e;
//        }
//
//        AuthLevel paymentAuthLevel = AuthLevel.valueOf(faceResult.getAuthLevel().name());
//
//        // 5. BLOCKED 처리
//        if (faceResult.isBlocked()) {
//            redisService.updateResult(requestId, null, "BLOCK", null, "얼굴 인증 차단");
//            redisService.updateStatus(requestId, PaymentRequestStatus.BLOCKED);
//            log.warn("결제 차단: requestId={}, similarity={}", requestId, faceResult.getSimilarity());
//            return ProcessPaymentResponse.builder()
//                    .requestId(requestId)
//                    .status("BLOCKED")
//                    .nextAction("BLOCK")
//                    .storeId(data.getStoreId())
//                    .amount(data.getAmount())
//                    .similarity((double) faceResult.getSimilarity())
//                    .build();
//        }
//
//        // 6. User 조회
//        User user = userRepository.findByUserId(faceResult.getBestUserId())
//                .orElseThrow(() -> {
//                    failRequest(requestId, "얼굴 인식된 사용자를 찾을 수 없습니다.");
//                    return new NotFoundException("얼굴 인식된 사용자를 찾을 수 없습니다.");
//                });
//
//        redisService.updateResult(requestId, user.getUserNo(), faceResult.getNextAction(), null, null);
//
//        // 7. 결제수단 조회 (isFacePay=true)
//        PaymentMethod paymentMethod = paymentMethodRepository
//                .findByUserNoAndIsFacePayTrueAndIsActiveTrue(user.getUserNo())
//                .orElseThrow(() -> {
//                    failRequest(requestId, "페이스페이 결제 수단이 등록되지 않았습니다.");
//                    return new NotFoundException("페이스페이 결제 수단이 등록되지 않았습니다.");
//                });
//
//        if (paymentMethod.getMethodType() != MethodType.ACCOUNT) {
//            failRequest(requestId, "계좌 결제 수단만 지원합니다.");
//            throw new BadRequestException("계좌 결제 수단만 지원합니다.");
//        }
//
//        // 8. 2차 인증 필요 처리
//        if (!"PASS".equals(faceResult.getNextAction())) {
//            Payment payment = buildBasePayment(user.getUserNo(), data, paymentMethod, paymentAuthLevel, faceResult);
//            payment = paymentRepository.save(payment);
//
//            redisService.updateResult(requestId, user.getUserNo(), faceResult.getNextAction(),
//                    payment.getPaymentId(), null);
//            redisService.updateStatus(requestId, PaymentRequestStatus.FAILED);
//
//            log.info("2차 인증 필요: requestId={}, authLevel={}", requestId, paymentAuthLevel);
//            return ProcessPaymentResponse.builder()
//                    .requestId(requestId)
//                    .paymentId(payment.getPaymentId())
//                    .status("FAILED")
//                    .nextAction(faceResult.getNextAction())
//                    .storeId(data.getStoreId())
//                    .amount(data.getAmount())
//                    .similarity((double) faceResult.getSimilarity())
//                    .build();
//        }
//
//        // 9. FDS 룰 평가 (실패 시 NONE fallback — FDS 오류로 결제를 막지 않음)
//        FdsEvaluationResult fdsResult;
//        try {
//            fdsResult = evaluateFds(user.getUserNo(), data.getStoreId(), data.getAmount());
//        } catch (Exception e) {
//            log.error("[FDS] 평가 실패, NONE으로 fallback: requestId={}, error={}", requestId, e.getMessage(), e);
//            fdsResult = new FdsEvaluationResult(0, List.of(), FdsAction.NONE);
//        }
//
//        FdsAction fdsAction = fdsResult.getAction();
//
//        if (fdsAction == FdsAction.BLOCK || fdsAction == FdsAction.PAUSE) {
//            Payment fdsPayment = buildBasePayment(user.getUserNo(), data, paymentMethod, paymentAuthLevel, faceResult);
//            fdsPayment.updateFds(fdsResult.getAnomalyScore(), fdsAction);
//
//            String fdsStatus = fdsAction == FdsAction.BLOCK ? "BLOCKED" : "PAUSED";
//            if (fdsAction == FdsAction.BLOCK) {
//                fdsPayment.updateStatus(PaymentStatus.BLOCKED);
//                redisService.updateStatus(requestId, PaymentRequestStatus.BLOCKED);
//            } else {
//                redisService.updateStatus(requestId, PaymentRequestStatus.FAILED);
//            }
//            fdsPayment = paymentRepository.save(fdsPayment);
//            fdsRuleService.saveLog(fdsPayment.getPaymentId(), user.getUserNo(), fdsResult);
//
//            redisService.updateResult(requestId, user.getUserNo(), fdsStatus, fdsPayment.getPaymentId(),
//                    "FDS 이상거래 탐지: " + fdsAction);
//            log.warn("[FDS] 결제 {}: requestId={}, score={}, action={}", fdsStatus, requestId,
//                    fdsResult.getAnomalyScore(), fdsAction);
//
//            return ProcessPaymentResponse.builder()
//                    .requestId(requestId)
//                    .paymentId(fdsPayment.getPaymentId())
//                    .status(fdsStatus)
//                    .nextAction(fdsAction.name())
//                    .storeId(data.getStoreId())
//                    .amount(data.getAmount())
//                    .similarity((double) faceResult.getSimilarity())
//                    .fdsScore(fdsResult.getAnomalyScore())
//                    .fdsAction(fdsAction.name())
//                    .failureReason("FDS 이상거래 탐지: " + fdsAction)
//                    .build();
//        }
//
//        // 10. 출금/입금 계좌 번호 조회
//        if (paymentMethod.getAccountId() == null) {
//            failRequest(requestId, "결제 수단에 계좌가 연결되지 않았습니다.");
//            throw new BadRequestException("결제 수단에 계좌가 연결되지 않았습니다.");
//        }
//        if (store.getAccountId() == null) {
//            failRequest(requestId, "매장에 입금 계좌가 설정되지 않았습니다.");
//            throw new BadRequestException("매장에 입금 계좌가 설정되지 않았습니다.");
//        }
//
//        Account withdrawalAccount = accountRepository.findById(paymentMethod.getAccountId())
//                .orElseThrow(() -> {
//                    failRequest(requestId, "출금 계좌를 찾을 수 없습니다.");
//                    return new NotFoundException("출금 계좌를 찾을 수 없습니다.");
//                });
//
//        Account depositAccount = accountRepository.findById(store.getAccountId())
//                .orElseThrow(() -> {
//                    failRequest(requestId, "매장 입금 계좌를 찾을 수 없습니다.");
//                    return new NotFoundException("매장 입금 계좌를 찾을 수 없습니다.");
//                });
//
//        // 10. SSAFY 계좌이체 API 호출
//        Map<String, Object> transferBody = ssafyApiClient.buildBody(
//                ssafyHeaderFactory.create("updateDemandDepositAccountTransfer", user.getUserKey()),
//                "depositAccountNo", depositAccount.getAccountNo(),
//                "depositTransactionSummary", store.getStoreName() + " 결제",
//                "transactionBalance", data.getAmount().toString(),
//                "withdrawalAccountNo", withdrawalAccount.getAccountNo(),
//                "withdrawalTransactionSummary", store.getStoreName() + " 페이스페이 결제"
//        );
//
//        Map<String, Object> transferResponse;
//        try {
//            transferResponse = ssafyApiClient.post(TRANSFER_API, transferBody);
//        } catch (Exception e) {
//            failRequest(requestId, "SSAFY API 오류: " + e.getMessage());
//            log.error("SSAFY 이체 실패: requestId={}, error={}", requestId, e.getMessage(), e);
//            return ProcessPaymentResponse.builder()
//                    .requestId(requestId)
//                    .status("FAILED")
//                    .storeId(data.getStoreId())
//                    .amount(data.getAmount())
//                    .failureReason("결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
//                    .build();
//        }
//
//        // 11. 거래 ID 추출 + 중복 감지 (이체 이미 완료 — throw 금지, 로그 경고만)
//        List<Map<String, Object>> recList = (List<Map<String, Object>>) transferResponse.get("REC");
//        String ssafyTransactionId = extractWithdrawalTransactionNo(recList);
//
//        if (ssafyTransactionId != null && paymentRepository.existsBySsafyTransactionId(ssafyTransactionId)) {
//            log.error("중복 거래 ID 감지 (이체 이미 완료됨, 정상 처리 속행): requestId={}, txnId={}", requestId, ssafyTransactionId);
//        }
//        if (ssafyTransactionId == null) {
//            log.warn("SSAFY 거래 ID를 추출할 수 없음: requestId={}", requestId);
//        }
//
//        // 12. 포인트 계산 + Payment 저장
//        int earnedPoints = (int) (data.getAmount() * pointRate);
//
//        Payment payment = buildBasePayment(user.getUserNo(), data, paymentMethod, paymentAuthLevel, faceResult);
//        payment.addEarnedPoints(earnedPoints);
//        payment.updateStatus(PaymentStatus.SUCCESS);
//        payment.updateSsafyTransactionId(ssafyTransactionId);
//        payment.updateFds(fdsResult.getAnomalyScore(), fdsAction);
//        payment = paymentRepository.save(payment);
//        fdsRuleService.saveLog(payment.getPaymentId(), user.getUserNo(), fdsResult);
//
//        // 13. TransactionLog 저장
//        long balanceAfter;
//        try {
//            balanceAfter = getBalanceAfter(user.getUserKey(), withdrawalAccount.getAccountNo());
//        } catch (Exception e) {
//            log.warn("잔액 조회 실패 (결제는 정상 처리됨): requestId={}", requestId, e);
//            balanceAfter = 0L;
//        }
//        transactionLogRepository.save(TransactionLog.builder()
//                .accountId(withdrawalAccount.getAccountId())
//                .transactionType(TransactionType.WITHDRAW)
//                .amount(data.getAmount())
//                .balanceAfter(balanceAfter)
//                .counterpart(depositAccount.getAccountNo())
//                .memo(store.getStoreName() + " 페이스페이 결제")
//                .ssafyTransactionId(ssafyTransactionId)
//                .build());
//
//        // 14. 포인트 적립 (실패해도 결제는 유지 — SSAFY 이체 이미 완료)
//        if (earnedPoints > 0) {
//            try {
//                pointService.earnPoints(user.getUserNo(), PointEarnRequest.builder()
//                        .amount((long) earnedPoints)
//                        .description(store.getStoreName() + " 페이스페이 결제")
//                        .paymentId(payment.getPaymentId())
//                        .build());
//            } catch (Exception e) {
//                log.error("포인트 적립 실패 (결제는 정상 처리됨): requestId={}, userNo={}", requestId, user.getUserNo(), e);
//                earnedPoints = 0;
//            }
//        }
//
//        // 15. Redis 상태 업데이트: DB 커밋 성공 후에만 SUCCESS로 변경
//        final int finalEarnedPoints = earnedPoints;
//        final Long finalPaymentId = payment.getPaymentId();
//        if (TransactionSynchronizationManager.isSynchronizationActive()) {
//            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//                @Override
//                public void afterCommit() {
//                    updateRedisSuccess(requestId, user.getUserNo(), finalPaymentId, data.getAmount(), finalEarnedPoints);
//                }
//            });
//        } else {
//            updateRedisSuccess(requestId, user.getUserNo(), finalPaymentId, data.getAmount(), finalEarnedPoints);
//        }
//
//        return ProcessPaymentResponse.builder()
//                .requestId(requestId)
//                .paymentId(payment.getPaymentId())
//                .status("SUCCESS")
//                .nextAction("PASS")
//                .storeId(data.getStoreId())
//                .amount(data.getAmount())
//                .earnedPoints(earnedPoints)
//                .ssafyTransactionId(ssafyTransactionId)
//                .similarity((double) faceResult.getSimilarity())
//                .fdsScore(fdsResult.getAnomalyScore())
//                .fdsAction(fdsAction.name())
//                .build();
//    }
//
//    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────
//
//    private Payment buildBasePayment(Long userNo, PaymentRequestData data,
//                                     PaymentMethod paymentMethod, AuthLevel authLevel,
//                                     SearchResponse faceResult) {
//        return Payment.builder()
//                .userNo(userNo)
//                .storeId(data.getStoreId())
//                .paymentMethodId(paymentMethod.getPaymentMethodId())
//                .amount(data.getAmount())
//                .authMethod(AuthMethod.FACE_PAY)
//                .authLevel(authLevel)
//                .faceDistance(1.0 - faceResult.getSimilarity())
//                .livenessPass(true)
//                .build();
//    }
//
//    private void updateRedisSuccess(String requestId, Long userNo, Long paymentId, Long amount, int earnedPoints) {
//        try {
//            redisService.updateResult(requestId, userNo, "PASS", paymentId, null);
//            redisService.updateStatus(requestId, PaymentRequestStatus.SUCCESS);
//            log.info("결제 성공: requestId={}, paymentId={}, amount={}, points={}",
//                    requestId, paymentId, amount, earnedPoints);
//        } catch (Exception e) {
//            log.error("Redis 상태 업데이트 실패 (DB 커밋은 완료됨): requestId={}", requestId, e);
//        }
//    }
//
//    private FdsEvaluationResult evaluateFds(Long userNo, Long storeId, Long amount) {
//        LocalDateTime now = LocalDateTime.now();
//
//        int recentCount = paymentRepository.countByUserNoAndStatusAndPaidAfter(
//                userNo, PaymentStatus.SUCCESS, now.minusMinutes(10));
//
//        long sum30d = paymentRepository.sumAmountByUserNoAndStatusAndPaidAfter(userNo, PaymentStatus.SUCCESS, now.minusDays(30));
//        long dailyAvg = sum30d / 30;
//
//        FdsEvaluationRequest request = FdsEvaluationRequest.builder()
//                .userNo(userNo)
//                .storeId(storeId)
//                .amount(amount)
//                .paymentTime(now)
//                .recentPaymentCount(recentCount)
//                .dailyAverageAmount(dailyAvg)
//                .build();
//
//        return fdsRuleService.evaluate(request);
//    }
//
//    private void failRequest(String requestId, String reason) {
//        try {
//            redisService.updateResult(requestId, null, null, null, reason);
//            redisService.updateStatus(requestId, PaymentRequestStatus.FAILED);
//        } catch (Exception e) {
//            log.warn("Redis 상태 업데이트 실패: requestId={}", requestId, e);
//        }
//    }
//
//    private String extractWithdrawalTransactionNo(List<Map<String, Object>> recList) {
//        if (recList == null || recList.isEmpty()) return null;
//        return recList.stream()
//                .filter(rec -> "2".equals(rec.get("transactionType")))
//                .map(rec -> (String) rec.get("transactionUniqueNo"))
//                .findFirst()
//                .orElse(null);
//    }
//
//    @SuppressWarnings("unchecked")
//    private long getBalanceAfter(String userKey, String accountNo) {
//        Map<String, Object> balanceBody = ssafyApiClient.buildBody(
//                ssafyHeaderFactory.create("inquireDemandDepositAccountBalance", userKey),
//                "accountNo", accountNo
//        );
//        Map<String, Object> balanceResponse = ssafyApiClient.post(BALANCE_API, balanceBody);
//        Map<String, Object> rec = (Map<String, Object>) balanceResponse.get("REC");
//        if (rec == null) return 0L;
//        String balance = (String) rec.get("accountBalance");
//        if (balance == null) return 0L;
//        try {
//            return Long.parseLong(balance);
//        } catch (NumberFormatException e) {
//            return 0L;
//        }
//    }
//}
