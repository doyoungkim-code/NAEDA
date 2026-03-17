package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.payment.dto.request.PaymentLimitRequest;
import com.ssafy.naeda.domain.payment.dto.response.PaymentLimitResponse;
import com.ssafy.naeda.domain.payment.entity.PaymentLimit;
import com.ssafy.naeda.domain.payment.repository.PaymentLimitRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentLimitService {

    private final PaymentLimitRepository paymentLimitRepository;

    // 기본 한도
    private static final long DEFAULT_DAILY_LIMIT = 500_000L;              // 1일 50만
    private static final long DEFAULT_MONTHLY_LIMIT = 3_000_000L;          // 월 300만
    private static final long DEFAULT_SINGLE_TRANSACTION_LIMIT = 300_000L; // 1회 30만

    // 상한선
    private static final long MAX_DAILY_LIMIT = 5_000_000L;               // 1일 최대 500만
    private static final long MAX_MONTHLY_LIMIT = 30_000_000L;            // 월 최대 3000만
    private static final long MAX_SINGLE_TRANSACTION_LIMIT = 3_000_000L;  // 1회 최대 300만

    // 조회 (없으면 기본값 반환)
    public PaymentLimitResponse getLimit(Long userNo) {
        PaymentLimit paymentLimit = paymentLimitRepository.findByUserNo(userNo)
                .orElse(buildDefault(userNo));

        return PaymentLimitResponse.from(paymentLimit);
    }

    // 설정/수정 (upsert)
    @Transactional
    public PaymentLimitResponse setLimit(Long userNo, PaymentLimitRequest request) {
        validateMaxLimits(request);

        PaymentLimit limit = paymentLimitRepository.findByUserNo(userNo)
                .map(existing -> {
                    existing.updateLimits(
                            request.getDailyLimit(),
                            request.getMonthlyLimit(),
                            request.getSingleTransactionLimit()
                    );
                    return existing;
                })
                .orElseGet(() -> paymentLimitRepository.save(
                        PaymentLimit.builder()
                                .userNo(userNo)
                                .dailyLimit(request.getDailyLimit())
                                .monthlyLimit(request.getMonthlyLimit())
                                .singleTransactionLimit(request.getSingleTransactionLimit())
                                .build()
                ));

        return PaymentLimitResponse.from(limit);
    }

    // 회원가입 시 기본 한도 자동 생성
    @Transactional
    public void createDefaultLimit(Long userNo) {
        if (paymentLimitRepository.findByUserNo(userNo).isEmpty()) {
            paymentLimitRepository.save(buildDefault(userNo));
        }
    }

    private PaymentLimit buildDefault(Long userNo) {
        return PaymentLimit.builder()
                .userNo(userNo)
                .dailyLimit(DEFAULT_DAILY_LIMIT)
                .monthlyLimit(DEFAULT_MONTHLY_LIMIT)
                .singleTransactionLimit(DEFAULT_SINGLE_TRANSACTION_LIMIT)
                .build();
    }

    private void validateMaxLimits(PaymentLimitRequest request) {
        if (request.getDailyLimit() > MAX_DAILY_LIMIT) {
            throw new BadRequestException("1일 한도는 최대 " + MAX_DAILY_LIMIT + "원까지 설정 가능합니다.");
        }
        if (request.getMonthlyLimit() > MAX_MONTHLY_LIMIT) {
            throw new BadRequestException("월 한도는 최대 " + MAX_MONTHLY_LIMIT + "원까지 설정 가능합니다.");
        }
        if (request.getSingleTransactionLimit() > MAX_SINGLE_TRANSACTION_LIMIT) {
            throw new BadRequestException("1회 한도는 최대 " + MAX_SINGLE_TRANSACTION_LIMIT + "원까지 설정 가능합니다.");
        }
    }
}