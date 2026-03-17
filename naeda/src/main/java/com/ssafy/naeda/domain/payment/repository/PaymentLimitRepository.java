package com.ssafy.naeda.domain.payment.repository;

import com.ssafy.naeda.domain.payment.entity.PaymentLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentLimitRepository extends JpaRepository<PaymentLimit, Long> {

    Optional<PaymentLimit> findByUserNo(Long userNo);
}
