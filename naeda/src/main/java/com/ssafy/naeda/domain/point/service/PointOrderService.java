package com.ssafy.naeda.domain.point.service;

import com.ssafy.naeda.domain.notification.entity.NotificationType;
import com.ssafy.naeda.domain.notification.entity.ReferenceType;
import com.ssafy.naeda.domain.point.dto.request.PointOrderCreateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointOrderResponse;
import com.ssafy.naeda.domain.point.entity.*;
import com.ssafy.naeda.domain.point.repository.*;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointOrderService {

    private final PointOrderRepository pointOrderRepository;
    private final PointProductRepository pointProductRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final FcmService fcmService;

    @Transactional
    public PointOrderResponse purchaseProduct(Long userNo, PointOrderCreateRequest request) {

        // 1. 상품 조회 (비관적 잠금) + 구매 가능 여부 검증
        PointProduct product = pointProductRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new NotFoundException("포인트 상품을 찾을 수 없습니다. id = " + request.getProductId()));

        if (!product.isAvailable()) {
            throw new IllegalStateException("현재 구매할 수 없는 상품입니다.");
        }

        // 2. 포인트 지갑 조회 (비관적 잠금)
        PointWallet wallet = pointWalletRepository.findByUserNoForUpdate(userNo)
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
                .addressId(request.getAddressId())
                .build();
        pointOrderRepository.save(order);

        // 7. 포인트 사용 알림 발송
        try {
            String formattedPoints = NumberFormat.getNumberInstance(Locale.KOREA)
                    .format(product.getPointPrice());

            Map<String, String> data = new HashMap<>();
            data.put("orderId", String.valueOf(order.getOrderId()));
            data.put("productName", product.getProductName());
            data.put("usedPoints", String.valueOf(product.getPointPrice()));

            fcmService.sendToUser(
                    userNo,
                    "포인트 사용",
                    product.getProductName() + " 교환에 " + formattedPoints + "P가 사용되었습니다.",
                    NotificationType.POINT,
                    order.getOrderId(),
                    ReferenceType.POINT,
                    data
            );
        } catch (Exception e) {
            log.error("포인트 사용 알림 발송 실패: userNo={}, orderId={}", userNo, order.getOrderId(), e);
        }

        // 8. 응답 반환
        return PointOrderResponse.from(product, order);
    }

    @Transactional(readOnly = true)
    public List<PointOrderResponse> getMyOrders(Long userNo) {
        List<PointOrder> orders = pointOrderRepository.findByUserNoOrderByOrderAtDesc(userNo);

        if (orders.isEmpty()) {
            return List.of();
        }

        // 상품 ID 목록 추출 후 한 번에 조회 (N+1 방지)
        List<Long> productIds = orders.stream()
                .map(PointOrder::getProductId)
                .distinct()
                .toList();
        Map<Long, PointProduct> productMap = pointProductRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(PointProduct::getProductId, Function.identity()));

        return orders.stream()
                .map(order -> {
                    PointProduct product = productMap.get(order.getProductId());
                    if (product == null) {
                        throw new NotFoundException("포인트 상품을 찾을 수 없습니다. id=" + order.getProductId());
                    }
                    return PointOrderResponse.from(product, order);
                })
                .toList();
    }
}