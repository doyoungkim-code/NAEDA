//package com.ssafy.naeda.domain.payment.service;
//
//import com.ssafy.naeda.domain.card.entity.CreditCard;
//import com.ssafy.naeda.domain.card.entity.DebitCard;
//import com.ssafy.naeda.domain.card.repository.CreditCardRepository;
//import com.ssafy.naeda.domain.card.repository.DebitCardRepository;
//import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
//import com.ssafy.naeda.domain.face.service.FaceService;
//import com.ssafy.naeda.domain.payment.dto.request.PaymentRequest;
//import com.ssafy.naeda.domain.payment.dto.response.PaymentResponse;
//import com.ssafy.naeda.domain.payment.entity.AuthLevel;
//import com.ssafy.naeda.domain.payment.entity.AuthMethod;
//import com.ssafy.naeda.domain.fds.entity.FdsAction;
//import com.ssafy.naeda.domain.payment.entity.MethodType;
//import com.ssafy.naeda.domain.payment.entity.Payment;
//import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
//import com.ssafy.naeda.domain.payment.entity.PaymentStatus;
//import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
//import com.ssafy.naeda.domain.payment.repository.PaymentRepository;
//import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
//import com.ssafy.naeda.domain.point.service.PointService;
//import com.ssafy.naeda.domain.store.entity.Store;
//import com.ssafy.naeda.domain.store.repository.StoreRepository;
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
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class PaymentService {
//
//    private static final String CREDIT_CARD_API = "/edu/creditCard/createCreditCardTransaction";
//
//    private final PaymentRepository paymentRepository;
//    private final PaymentMethodRepository paymentMethodRepository;
//    private final CreditCardRepository creditCardRepository;
//    private final DebitCardRepository debitCardRepository;
//    private final UserRepository userRepository;
//    private final StoreRepository storeRepository;
//    private final FaceService faceService;
//    private final PointService pointService;
//    private final SsafyApiClient ssafyApiClient;
//    private final SsafyHeaderFactory ssafyHeaderFactory;
//
//    @Value("${payment.point.rate:0.05}")
//    private double pointRate;
//
//    /**
//     * 페이스페이 결제 — 단말기 요청.
//     *
//     * 흐름:
//     * 1. 매장 조회 → facePayEnabled 확인
//     * 2. 얼굴 인증 + RBA 평가 → bestUserId로 사용자 특정
//     * 3. BLOCKED → 거부 (사용자 특정 불가 → Payment 미저장)
//     * 4. User 조회(bestUserId) → 페이스페이 결제 수단 자동 조회
//     * 5. 2차 인증 필요 → Payment(FAILED) 저장 후 반환
//     * 6. SSAFY 카드 결제 (cardNo, cvc, merchantId, paymentBalance)
//     * 7. Payment(SUCCESS) 저장 → 포인트 적립
//     */
//    @Transactional
//    public PaymentResponse pay(PaymentRequest request, MultipartFile faceImage) {
//        // 1. 매장 조회 및 페이스페이 지원 여부 확인
//        Store store = storeRepository.findById(request.getStoreId())
//                .orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));
//        if (!Boolean.TRUE.equals(store.getFacePayEnabled())) {
//            throw new BadRequestException("해당 매장은 페이스페이를 지원하지 않습니다.");
//        }
//
//        // 2. 얼굴 인증 + RBA 평가
//        SearchResponse faceResult = faceService.search(faceImage, 1, request.getAmount());
//        AuthLevel paymentAuthLevel = AuthLevel.valueOf(faceResult.getAuthLevel().name());
//
//        // 3. BLOCKED 처리 — 얼굴 매칭 실패, 사용자 특정 불가 → Payment 미저장
//        if (faceResult.isBlocked()) {
//            log.warn("결제 차단 (얼굴 불일치): storeId={}, similarity={}", request.getStoreId(), faceResult.getSimilarity());
//            return PaymentResponse.blocked(faceResult);
//        }
//
//        // 4. bestUserId → User 조회 → 페이스페이 결제 수단 조회
//        User user = userRepository.findByUserId(faceResult.getBestUserId())
//                .orElseThrow(() -> new NotFoundException("얼굴 인식된 사용자를 찾을 수 없습니다."));
//
//        PaymentMethod paymentMethod = paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(user.getUserNo())
//                .orElseThrow(() -> new NotFoundException("페이스페이 결제 수단이 등록되지 않았습니다."));
//
//        String[] cardInfo = resolveCardInfo(paymentMethod);
//        String cardNo = cardInfo[0];
//        String cvc    = cardInfo[1];
//
//        // 5. 2차 인증 필요 처리
//        if (!"PASS".equals(faceResult.getNextAction())) {
//            Payment payment = Payment.builder()
//                    .userNo(user.getUserNo())
//                    .storeId(request.getStoreId())
//                    .paymentMethodId(paymentMethod.getPaymentMethodId())
//                    .amount(request.getAmount())
//                    .authMethod(AuthMethod.FACE_PAY)
//                    .authLevel(paymentAuthLevel)
//                    .faceDistance(1.0 - faceResult.getSimilarity())
//                    .livenessPass(true)
//                    .build();
//            paymentRepository.save(payment);
//            log.info("2차 인증 필요: userNo={}, authLevel={}", user.getUserNo(), paymentAuthLevel);
//            return PaymentResponse.from(payment, "REQUIRE_SECOND_FACTOR", faceResult);
//        }
//
//        // 6. SSAFY 카드 결제
//        Long ssafyMerchantId = store.resolveSsafyMerchantId();
//        if (ssafyMerchantId == null) {
//            throw new BadRequestException("해당 매장에 SSAFY merchantId가 연결되어 있지 않습니다.");
//        }
//
//        Map<String, Object> header = ssafyHeaderFactory.create("createCreditCardTransaction", user.getUserKey());
//        Map<String, Object> body = ssafyApiClient.buildBody(header,
//                "cardNo",         cardNo,
//                "cvc",            cvc,
//                "merchantId",     ssafyMerchantId.toString(),
//                "paymentBalance", request.getAmount().toString()
//        );
//        Map<String, Object> ssafyResponse = ssafyApiClient.post(CREDIT_CARD_API, body);
//        String transactionId = extractTransactionId(ssafyResponse);
//
//        // 6-1. 중복 결제 방지 (동일 transactionId)
//        if (transactionId != null && paymentRepository.existsBySsafyTransactionId(transactionId)) {
//            throw new BadRequestException("이미 처리된 결제입니다: " + transactionId);
//        }
//
//        // 7. 포인트 계산 및 Payment 저장
//        int earnedPoints = (int) (request.getAmount() * pointRate);
//
//        Payment payment = Payment.builder()
//                .userNo(user.getUserNo())
//                .storeId(request.getStoreId())
//                .paymentMethodId(paymentMethod.getPaymentMethodId())
//                .amount(request.getAmount())
//                .authMethod(AuthMethod.FACE_PAY)
//                .authLevel(paymentAuthLevel)
//                .faceDistance(1.0 - faceResult.getSimilarity())
//                .livenessPass(true)
//                .earnedPoints(earnedPoints)
//                .build();
//        payment.updateStatus(PaymentStatus.SUCCESS);
//        payment.updateSsafyTransactionId(transactionId);
//        payment = paymentRepository.save(payment);
//
//        // 8. 포인트 적립
//        if (earnedPoints > 0) {
//            pointService.earnPoints(user.getUserNo(), PointEarnRequest.builder()
//                    .amount((long) earnedPoints)
//                    .description(store.getStoreName() + " 페이스페이 결제")
//                    .paymentId(payment.getPaymentId())
//                    .build());
//        }
//
//        log.info("결제 성공: paymentId={}, userNo={}, amount={}, points={}",
//                payment.getPaymentId(), user.getUserNo(), request.getAmount(), earnedPoints);
//        return PaymentResponse.from(payment, "PASS", faceResult);
//    }
//
//    /**
//     * 결제 내역 목록 조회 (from/to 지정 시 기간 필터링).
//     */
//    public List<PaymentResponse> getPayments(Long userNo, LocalDateTime from, LocalDateTime to) {
//        List<Payment> payments;
//        if (from != null && to != null) {
//            payments = paymentRepository.findByUserNoAndPaidBetweenOrderByPaidDesc(userNo, from, to);
//        } else {
//            payments = paymentRepository.findByUserNoOrderByPaidDesc(userNo);
//        }
//        return payments.stream().map(PaymentResponse::from).toList();
//    }
//
//    /**
//     * 결제 단건 조회.
//     */
//    public PaymentResponse getPayment(Long userNo, Long paymentId) {
//        Payment payment = paymentRepository.findById(paymentId)
//                .filter(p -> p.getUserNo().equals(userNo))
//                .orElseThrow(() -> new NotFoundException("존재하지 않는 결제 내역입니다."));
//        return PaymentResponse.from(payment);
//    }
//
//    // ── 헬퍼 ────────────────────────────────────────────────────────────────
//
//    private String[] resolveCardInfo(PaymentMethod paymentMethod) {
//        if (paymentMethod.getMethodType() == MethodType.CREDIT_CARD) {
//            CreditCard card = creditCardRepository.findById(paymentMethod.getCreditCardId())
//                    .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
//                    .orElseThrow(() -> new NotFoundException("신용카드 정보를 찾을 수 없습니다."));
//            return new String[]{card.getCardNo(), card.getCvc()};
//        } else if (paymentMethod.getMethodType() == MethodType.DEBIT_CARD) {
//            DebitCard card = debitCardRepository.findById(paymentMethod.getDebitCardId())
//                    .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
//                    .orElseThrow(() -> new NotFoundException("체크카드 정보를 찾을 수 없습니다."));
//            return new String[]{card.getCardNo(), card.getCvc()};
//        }
//        throw new BadRequestException("카드 결제 수단만 지원합니다. (ACCOUNT 타입 불가)");
//    }
//
//    @SuppressWarnings("unchecked")
//    private String extractTransactionId(Map<String, Object> response) {
//        Object rec = response.get("REC");
//        if (rec instanceof Map) {
//            return (String) ((Map<String, Object>) rec).get("transactionUniqueNo");
//        }
//        return null;
//    }
//}
