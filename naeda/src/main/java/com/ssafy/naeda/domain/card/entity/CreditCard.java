package com.ssafy.naeda.domain.card.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_card")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class CreditCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_card_id")
    private Long creditCardId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "card_no", nullable = false, unique = true, length = 50)
    private String cardNo;

    @Column(length = 3, nullable = false)
    private String cvc;

    @Column(name = "card_unique_no", length = 50, nullable = false)
    private String cardUniqueNo;

    @Column(name = "card_issuer_code", length = 10, nullable = false)
    private String cardIssuerCode;

    @Column(name = "card_issuer_name", length = 50, nullable = false)
    private String cardIssuerName;

    @Column(name = "card_name", length = 100, nullable = false)
    private String cardName;

    @Column(name = "card_expiry_date", length = 8, nullable = false)
    private String cardExpiryDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "credit_limit", nullable = false)
    private Long creditLimit;

    @Column(name = "billing_date", nullable = false)
    private Integer billingDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    public void deactivate() {
        this.isActive = false;
    }
}
