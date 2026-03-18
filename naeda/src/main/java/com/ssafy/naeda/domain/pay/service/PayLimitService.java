package com.ssafy.naeda.domain.pay.service;

import com.ssafy.naeda.domain.pay.dto.request.PayLimitRequest;
import com.ssafy.naeda.domain.pay.dto.response.PayLimitResponse;
import com.ssafy.naeda.domain.pay.entity.PayLimit;
import com.ssafy.naeda.domain.pay.repository.PayLimitRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayLimitService {

    private final PayLimitRepository payLimitRepository;

    private static final long DEFAULT_DAILY = 500_000L;
    private static final long DEFAULT_MONTHLY = 3_000_000L;
    private static final long DEFAULT_SINGLE = 300_000L;
    private static final long MAX_DAILY = 5_000_000L;
    private static final long MAX_MONTHLY = 30_000_000L;
    private static final long MAX_SINGLE = 3_000_000L;

    public PayLimitResponse getLimit(Long userNo) {
        return payLimitRepository.findByUserNo(userNo)
                .map(PayLimitResponse::from)
                .orElse(new PayLimitResponse(userNo, DEFAULT_DAILY, DEFAULT_MONTHLY, DEFAULT_SINGLE));
    }

    @Transactional
    public PayLimitResponse setLimit(Long userNo, PayLimitRequest request) {
        validateMaxLimits(request);

        PayLimit limit = payLimitRepository.findByUserNo(userNo)
                .map(existing -> {
                    existing.updateLimits(
                            request.getDailyLimit(),
                            request.getMonthlyLimit(),
                            request.getSingleTransactionLimit());
                    return existing;
                })
                .orElseGet(() -> payLimitRepository.save(PayLimit.builder()
                        .userNo(userNo)
                        .dailyLimit(request.getDailyLimit())
                        .monthlyLimit(request.getMonthlyLimit())
                        .singleTransactionLimit(request.getSingleTransactionLimit())
                        .build()));

        return PayLimitResponse.from(limit);
    }

    @Transactional
    public void createDefaultLimit(Long userNo) {
        if (payLimitRepository.findByUserNo(userNo).isEmpty()) {
            payLimitRepository.save(PayLimit.builder()
                    .userNo(userNo)
                    .dailyLimit(DEFAULT_DAILY)
                    .monthlyLimit(DEFAULT_MONTHLY)
                    .singleTransactionLimit(DEFAULT_SINGLE)
                    .build());
        }
    }

    private void validateMaxLimits(PayLimitRequest request) {
        if (request.getDailyLimit() > MAX_DAILY) {
            throw new BadRequestException("1일 한도는 최대 " + MAX_DAILY + "원까지 설정 가능합니다.");
        }
        if (request.getMonthlyLimit() > MAX_MONTHLY) {
            throw new BadRequestException("월 한도는 최대 " + MAX_MONTHLY + "원까지 설정 가능합니다.");
        }
        if (request.getSingleTransactionLimit() > MAX_SINGLE) {
            throw new BadRequestException("1회 한도는 최대 " + MAX_SINGLE + "원까지 설정 가능합니다.");
        }
    }
}
