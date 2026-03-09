package com.ssafy.naeda.domain.payment.repository;

import com.ssafy.naeda.domain.payment.entity.Payment;
import com.ssafy.naeda.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserNoOrderByPaidDesc(Long userNo);

    List<Payment> findByUserNoAndPaidBetweenOrderByPaidDesc(Long userNo, LocalDateTime from, LocalDateTime to);

    List<Payment> findByStoreIdOrderByPaidDesc(Long storeId);

    List<Payment> findByUserNoAndStatus(Long userNo, PaymentStatus status);
}
