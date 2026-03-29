//package com.ssafy.naeda.domain.payment.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "payment_limit")
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor(access = AccessLevel.PRIVATE)
//@Getter
//@Builder
//public class PaymentLimit {
//
//    @Id
//    @Column(name = "payment_limit_id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long paymentLimitId;
//
//    @Column(name = "user_no", nullable = false, unique = true)
//    private Long userNo;
//
//    @Column(name = "daily_limit", nullable = false)
//    private Long dailyLimit;
//
//    @Column(name = "monthly_limit", nullable = false)
//    private Long monthlyLimit;
//
//    @Column(name = "single_transaction_limit", nullable = false)
//    private Long singleTransactionLimit;
//
//    @CreationTimestamp
//    @Column(nullable = false, updatable = false)
//    private LocalDateTime created;
//
//    private LocalDateTime modified;
//
//    public void updateLimits(Long dailyLimit, Long monthlyLimit, Long singleTransactionLimit) {
//        this.dailyLimit = dailyLimit;
//        this.monthlyLimit = monthlyLimit;
//        this.singleTransactionLimit = singleTransactionLimit;
//        this.modified = LocalDateTime.now();
//    }
//
//}
