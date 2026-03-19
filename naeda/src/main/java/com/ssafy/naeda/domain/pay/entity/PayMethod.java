package com.ssafy.naeda.domain.pay.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_method")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class PayMethod {
    @Id
    @Column(name = "payment_method_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentMethodId;

    @Column(name = "user_no",nullable = false)
    private Long userNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false)
    private MethodType methodType;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "debit_card_id")
    private Long debitCardId;

    @Column(name = "credit_card_id")
    private Long creditCardId;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_face_pay")
    @Builder.Default
    private Boolean isFacePay = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    public void setAsFacePay(){
        this.isDefault = true;
        this.isFacePay = true;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void clearDefault() {
        this.isDefault = false;
    }

    public void clearFacePay() {
        this.isFacePay = false;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
