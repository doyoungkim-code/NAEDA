package com.ssafy.naeda.domain.payment.service;

import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentRequestService {

    private final PaymentRequestRedisService redisService;
    private final StoreRepository storeRepository;

    /**
     * 결제 요청 생성.
     * 매장 존재 + facePayEnabled 검증 후 Redis에 PENDING 상태로 저장.
     */
    public PaymentRequestData createPaymentRequest(Long storeId, Long amount) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));

        if (!Boolean.TRUE.equals(store.getFacePayEnabled())) {
            throw new BadRequestException("해당 매장은 페이스페이를 지원하지 않습니다.");
        }

        return redisService.createRequest(storeId, amount);
    }

    /**
     * 결제 요청 단건 조회 (폴링).
     * 만료/미존재 시 NotFoundException.
     */
    public PaymentRequestData getPaymentRequest(String requestId) {
        PaymentRequestData data = redisService.getRequest(requestId);
        if (data == null) {
            throw new NotFoundException("결제 요청이 만료되었거나 존재하지 않습니다.");
        }
        return data;
    }

    /**
     * 매장별 활성 결제 요청 목록 조회.
     */
    public List<PaymentRequestData> getPaymentRequestsByStore(Long storeId) {
        return redisService.getRequestsByStore(storeId);
    }
}
