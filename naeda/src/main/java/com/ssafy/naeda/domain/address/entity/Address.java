package com.ssafy.naeda.domain.address.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "address")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "address_name", length = 50)
    private String addressName;

    @Column(length = 50, nullable = false)
    private String recipient;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(name = "road_address", nullable = false)
    private String roadAddress;

    @Column(name = "number_address")
    private String numberAddress;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    public void update(String addressName, String recipient, String phone,
                       String roadAddress, String numberAddress,
                       String detailAddress, String zipCode) {
        this.addressName = addressName;
        this.recipient = recipient;
        this.phone = phone;
        this.roadAddress = roadAddress;
        this.numberAddress = numberAddress;
        this.detailAddress = detailAddress;
        this.zipCode = zipCode;
    }

    public void updateDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
