package com.ssafy.naeda.domain.card.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "debit_card")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class DebitCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "debit_card_id")
    private Long debitCardId;

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

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    public void deactivate() {
        this.isActive = false;
    }
}
