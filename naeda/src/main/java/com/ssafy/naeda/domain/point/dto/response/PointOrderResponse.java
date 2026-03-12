package com.ssafy.naeda.domain.point.dto.response;

import com.ssafy.naeda.domain.point.entity.PointOrder;
import com.ssafy.naeda.domain.point.entity.PointProduct;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class PointOrderResponse {

    private Long orderId;
    private Long userNo;
    private Long productId;
    private String productName;
    private Long pointPrice;
    private String roadAddress;
    private String numberAddress;
    private LocalDateTime orderAt;

    public static PointOrderResponse from (PointProduct product, PointOrder order) {
        return PointOrderResponse.builder()
                .orderId(order.getOrderId())
                .userNo(order.getUserNo())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .pointPrice(product.getPointPrice())
                .roadAddress(order.getRoadAddress())
                .numberAddress(order.getNumberAddress())
                .orderAt(order.getOrderAt())
                .build();
    }
}