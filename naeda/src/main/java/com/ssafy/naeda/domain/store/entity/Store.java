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

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "store_name", length = 100, nullable = false)
    private String storeName;

    @Column(length = 30, nullable = false)
    private String category;

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
