package com.ssafy.naeda.domain.pay.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pay_transaction", indexes = {
        @Index(name = "idx_pay_tx_user", columnList = "userNo"),
        @Index(name = "idx_pay_tx_store", columnList = "storeId"),
        @Index(name = "idx_pay_tx_idempotency", columnList = "idempotencyKey", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PayTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userNo;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long paymentMethodId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayStatus status;

    // 인증 정보
    @Column(nullable = false, length = 20)
    private String authMethod;       // CARD, ACCOUNT

    @Column(nullable = false, length = 20)
    private String authLevel;        // FACE_ONLY, FACE_PHONE, FACE_SIGNATURE, BLOCKED

    private Double faceDistance;
    private Boolean livenessPass;
    private Boolean pinVerified;

    // FDS 정보
    private Integer fdsScore;

    @Column(length = 10)
    private String fdsAction;        // NONE, ALERT, PAUSE, BLOCK

    private String fdsReason;

    // 결과 정보
    private String ssafyTransactionId;
    private Long earnedPoints;
    private String failureReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // === 상태 변경 메서드 (도메인 로직) ===

    public void markSuccess(String ssafyTransactionId, Long earnedPoints){
        this.status = PayStatus.SUCCESS;
        this.ssafyTransactionId = ssafyTransactionId;
        this.earnedPoints = earnedPoints;
    }

    public void markFailed(String failureReason){
        this.status = PayStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void markBlocked(String failureReason) {
        this.status = PayStatus.BLOCKED;
        this.failureReason = failureReason;
    }

    public void markPaused(String fdsReason) {
        this.status = PayStatus.PAUSED;
        this.fdsReason = fdsReason;
    }

    public void applyFds(int fdsScore, String fdsAction) {
        this.fdsScore = fdsScore;
        this.fdsAction = fdsAction;
    }

}
