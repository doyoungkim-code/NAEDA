package com.ssafy.naeda.domain.point.dto.response;

import com.ssafy.naeda.domain.point.entity.PointProduct;
import com.ssafy.naeda.domain.point.entity.PointProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointProductResponse {

    private Long productId;
    private String productName;
    private String description;
    private String category;
    private String imageUrl;
    private Long pointPrice;
    private Integer stockQuantity;
    private PointProductStatus status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    public static PointProductResponse from (PointProduct product) {
        return builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .pointPrice(product.getPointPrice())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .startsAt(product.getStartsAt())
                .endsAt(product.getEndsAt())
                .build();
    }
}
