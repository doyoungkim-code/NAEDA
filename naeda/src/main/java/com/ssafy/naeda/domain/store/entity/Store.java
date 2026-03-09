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
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "store_name", length = 100, nullable = false)
    private String storeName;

    @Column(name = "category", length = 30, nullable = false)
    private String categoryId;     // SSAFY 카테고리 고유 ID (예: "CG-4fa85f6425ad1d3")

    @Column(name = "category_name", length = 50)
    private String categoryName;   // SSAFY 카테고리명 (예: "대형마트")

    @Column(name = "road_address", length = 255, nullable = false)
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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;
}
