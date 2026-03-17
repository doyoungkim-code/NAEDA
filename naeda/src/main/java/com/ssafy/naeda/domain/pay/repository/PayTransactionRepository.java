package com.ssafy.naeda.domain.pay.repository;

import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayTransactionRepository extends JpaRepository<PayTransaction, Long> {
    List<PayTransaction> findByUserNoOrderByCreatedAtDesc(Long userNo);

    List<PayTransaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    Optional<PayTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

}
