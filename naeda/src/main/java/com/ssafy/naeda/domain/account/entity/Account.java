package com.ssafy.naeda.domain.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "account_no", length = 50, nullable = false, unique = true)
    private String accountNo;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created;
}
