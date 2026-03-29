package com.ssafy.naeda.domain.point.entity;

import com.ssafy.naeda.global.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_wallet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class PointWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "user_no", nullable = false, unique = true)
    private Long userNo;

    @Column(nullable = false)
    @Builder.Default
    private Long balance = 0L;

    @Column(name = "total_earned", nullable = false)
    @Builder.Default
    private Long totalEarned = 0L;

    @Column(name = "total_used", nullable = false)
    @Builder.Default
    private Long totalUsed = 0L;

    @Column(name = "updated")
    private LocalDateTime updated;

    public void earn(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 금액은 0보다 커야 합니다.");
        }
        if (Long.MAX_VALUE - this.balance < amount) {
            throw new IllegalStateException("포인트 잔액이 최대치를 초과합니다.");
        }
        if (Long.MAX_VALUE - this.totalEarned < amount) {
            throw new IllegalStateException("누적 적립 포인트가 최대치를 초과합니다.");
        }
        this.balance += amount;
        this.totalEarned += amount;
        this.updated = LocalDateTime.now();
    }

    public void use(Long amount) {
        if(this.balance < amount) {
            throw new InsufficientBalanceException("포인트 잔액이 부족합니다. 현재 잔액: " + this.balance);
        }
        if (Long.MAX_VALUE - this.totalUsed < amount) {
            throw new IllegalStateException("누적 사용 포인트가 최대치를 초과합니다.");
        }

        this.balance -= amount;
        this.totalUsed += amount;
        this.updated = LocalDateTime.now();
    }

    public void charge(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        if (Long.MAX_VALUE - this.balance < amount) {
            throw new IllegalStateException("포인트 잔액이 최대치를 초과합니다.");
        }
        if (Long.MAX_VALUE - this.totalEarned < amount) {
            throw new IllegalStateException("누적 적립 포인트가 최대치를 초과합니다.");
        }
        this.balance += amount;
        this.totalEarned += amount;
        this.updated = LocalDateTime.now();
    }
}
