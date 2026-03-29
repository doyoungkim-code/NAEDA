package com.ssafy.naeda.domain.pay.repository;

import com.ssafy.naeda.domain.pay.entity.PayMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayMethodRepository extends JpaRepository<PayMethod, Long> {

    List<PayMethod> findByUserNoAndIsActiveTrue(Long userNo);

    List<PayMethod> findByUserNoAndIsFacePayTrueAndIsActiveTrue(Long userNo);

    Optional<PayMethod> findByCreditCardIdAndIsActiveTrue(Long creditCardId);

    List<PayMethod> findAllByCreditCardIdAndIsActiveTrue(Long creditCardId);

    Optional<PayMethod> findByDebitCardIdAndIsActiveTrue(Long debitCardId);

    List<PayMethod> findAllByDebitCardIdAndIsActiveTrue(Long debitCardId);

    void deleteByUserNo(Long userNo);
}
