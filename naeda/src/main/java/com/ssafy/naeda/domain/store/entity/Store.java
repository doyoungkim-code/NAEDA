package com.ssafy.naeda.domain.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;

@Entity
@Table(name = "store")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "ssafy_merchant_id", unique = true)
    private Long ssafyMerchantId;

    @Column(name = "user_no")
    private Long userNo;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "store_name", length = 100, nullable = false)
    private String storeName;

    @Column(name = "category", length = 30, nullable = false)
    private String categoryId;     // SSAFY 카테고리 고유 ID (예: "CG-4fa85f6425ad1d3")

    @Column(name = "category_name", length = 50)
    private String categoryName;   // SSAFY 카테고리명 (예: "대형마트")

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "number_address", length = 255)
    private String numberAddress;

    private Double latitude;

    private Double longitude;

    @Column(length = 20)
    private String phone;

    @Column(name = "is_local_business", nullable = false)
    @Builder.Default
    private Boolean isLocalBusiness = false;

    @Column(name = "face_pay_enabled", nullable = false)
    @Builder.Default
    private Boolean facePayEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Double rating = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    @Builder.Default
    private StoreSourceType sourceType = StoreSourceType.SSAFY;

    @Column(name = "source_key", length = 120, unique = true)
    private String sourceKey;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_enriched_at")
    private LocalDateTime lastEnrichedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    public void updatePublicCatalog(
            String storeName,
            String categoryId,
            String categoryName,
            String roadAddress,
            String numberAddress,
            Double latitude,
            Double longitude,
            String phone
    ) {
        this.storeName = storeName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.roadAddress = roadAddress;
        this.numberAddress = numberAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.isLocalBusiness = true;
        this.facePayEnabled = false;
        this.isActive = true;
    }

    public void assignCatalogIdentity(StoreSourceType sourceType, String sourceKey) {
        this.sourceType = sourceType;
        this.sourceKey = sourceKey;
    }

    public void assignSsafyIdentity(Long ssafyMerchantId) {
        this.sourceType = StoreSourceType.SSAFY;
        this.ssafyMerchantId = ssafyMerchantId;
        this.sourceKey = ssafyMerchantId == null ? null : "ssafy:" + ssafyMerchantId;
        this.facePayEnabled = true;
    }

    public void updateEnrichment(String imageUrl, String description, Double rating, LocalDateTime enrichedAt) {
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (description != null) {
            this.description = description;
        }
        this.rating = rating == null ? this.rating : rating;
        this.lastEnrichedAt = enrichedAt;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Long resolveSsafyMerchantId() {
        if (this.ssafyMerchantId != null) {
            return this.ssafyMerchantId;
        }
        if (this.sourceType == null || this.sourceType == StoreSourceType.SSAFY) {
            return this.storeId;
        }
        return null;
    }
}
