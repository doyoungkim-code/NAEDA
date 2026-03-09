package com.ssafy.naeda.domain.payment.repository;

import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // 사용자의 결제수단 목록 전체 조회
    List<PaymentMethod> findByUserNo(Long userNo);

    // 페이스페이 결제수단 조회
    Optional<PaymentMethod> findByUserNoAndIsFacePayTrue(Long userNo);

    List<PaymentMethod> findByCreditCardId(Long creditCardId);

    List<PaymentMethod> findByDebitCardId(Long debitCardId);
}
