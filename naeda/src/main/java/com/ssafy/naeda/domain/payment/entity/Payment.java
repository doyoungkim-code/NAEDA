package com.ssafy.naeda.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 20)
    private AuthMethod authMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_level", nullable = false, length = 20)
    private AuthLevel authLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.FAILED;

    @Column(name = "face_distance")
    private Double faceDistance;

    @Column(name = "liveness_passed")
    private Boolean livenessPass;

    @Column(name = "pin_verified", nullable = false)
    @Builder.Default
    private Boolean pinVerified = false;

    @Column(name = "fds_score", nullable = false)
    @Builder.Default
    private Integer fdsScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "fds_action", nullable = false, length = 10)
    @Builder.Default
    private FdsAction fdsAction = FdsAction.NONE;

    @Column(name = "earned_points", nullable = false)
    @Builder.Default
    private Integer earnedPoints = 0;

    @Column(name = "ssafy_transaction_id", length = 100)
    private String ssafyTransactionId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime paid;

    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }

    public void updateFds(int fdsScore, FdsAction fdsAction) {
        this.fdsScore = fdsScore;
        this.fdsAction = fdsAction;
    }

    public void updateSsafyTransactionId(String ssafyTransactionId) {
        this.ssafyTransactionId = ssafyTransactionId;
    }

    public void addEarnedPoints(int points) {
        this.earnedPoints = points;
    }
}
