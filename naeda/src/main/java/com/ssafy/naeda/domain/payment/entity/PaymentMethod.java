package com.ssafy.naeda.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_method")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "user_no", nullable = false)
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

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_face_pay", nullable = false)
    @Builder.Default
    private Boolean isFacePay = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created;

    public void setAsFacePay() {
        this.isDefault = true;
        this.isFacePay = true;
    }
}
