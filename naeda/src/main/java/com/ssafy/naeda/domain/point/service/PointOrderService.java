package com.ssafy.naeda.domain.point.service;

import com.ssafy.naeda.domain.point.dto.request.PointOrderCreateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointOrderResponse;
import com.ssafy.naeda.domain.point.entity.*;
import com.ssafy.naeda.domain.point.repository.*;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointOrderService {

    private final PointOrderRepository pointOrderRepository;
    private final PointProductRepository pointProductRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public PointOrderResponse purchaseProduct(Long userNo, PointOrderCreateRequest request) {

        // 1. 상품 조회 + 구매 가능 여부 검증
        PointProduct product = pointProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("포인트 상품을 찾을 수 없습니다. id = " + request.getProductId()));

        if (!product.isAvailable()) {
            throw new IllegalStateException("현재 구매할 수 없는 상품입니다.");
        }

        // 2. 포인트 지갑 조회
        PointWallet wallet = pointWalletRepository.findByUserNo(userNo)
                .orElseThrow(() -> new NotFoundException("포인트 지갑을 찾을 수 없습니다. userNo=" + userNo));

        // 3. 포인트 차감 (잔액 부족하면 use() 내부에서 예외 발생)
        wallet.use(product.getPointPrice());

        // 4. 재고 차감 (재고 0이면 자동 SOLD_OUT)
        product.deductStock(1);

        // 5. 포인트 이력 기록 (USE_COUPON)
        PointHistory history = PointHistory.builder()
                .walletId(wallet.getWalletId())
                .type(PointType.USE_COUPON)
                .amount(product.getPointPrice())
                .balanceAfter(wallet.getBalance())
                .description(product.getProductName() + " 교환")
                .build();
        pointHistoryRepository.save(history);

        // 6. 주문 생성
        PointOrder order = PointOrder.builder()
                .userNo(userNo)
                .productId(product.getProductId())
                .roadAddress(request.getRoadAddress())
                .numberAddress(request.getNumberAddress())
                .build();
        pointOrderRepository.save(order);

        // 7. 응답 반환
        return PointOrderResponse.from(product, order);
    }

    @Transactional(readOnly = true)
    public List<PointOrderResponse> getMyOrders(Long userNo) {
        List<PointOrder> orders = pointOrderRepository.findByUserNo(userNo);

        return orders.stream()
                .sorted(Comparator.comparing(PointOrder::getOrderAt).reversed())
                .map(order -> {
                    PointProduct product = pointProductRepository.findById(order.getProductId())
                            .orElseThrow(() -> new NotFoundException("포인트 상품을 찾을 수 없습니다. id=" + order.getProductId()));

                    return PointOrderResponse.from(product, order);
                })
                .toList();
    }
}