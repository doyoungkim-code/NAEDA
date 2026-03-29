package com.ssafy.naeda.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PointProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 120, nullable = false)
    private String productName;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "point_price", nullable = false)
    private Long pointPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PointProductStatus status = PointProductStatus.ON_SALE;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    public void update(String productName, String description, String category,
                       String imageUrl, Long pointPrice, Integer stockQuantity,
                       PointProductStatus status, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.productName = productName;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.pointPrice = pointPrice;
        this.stockQuantity = stockQuantity;
        this.status = status;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    // — 비즈니스 메서드 —
    
    public boolean isAvailable() {
        if (this.status != PointProductStatus.ON_SALE) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (this.startsAt != null && now.isBefore(this.startsAt)) {
            return false;
        }
        if (this.endsAt != null && now.isAfter(this.endsAt)) {
            return false;
        }
        return true;
    }

    public void deductStock(int quantity) {
        if (this.status != PointProductStatus.ON_SALE) {
            throw new IllegalStateException("판매 중인 상품만 재고 차감이 가능합니다.");
        }
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + this.stockQuantity);
        }
        this.stockQuantity -= quantity;
        if (this.stockQuantity == 0) {
            this.status = PointProductStatus.SOLD_OUT;
        }
    }
}
