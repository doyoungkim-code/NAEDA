package com.ssafy.naeda.domain.pay.repository;

import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PayTransactionRepository extends JpaRepository<PayTransaction, Long> {
    List<PayTransaction> findByUserNoOrderByCreatedAtDesc(Long userNo);

    List<PayTransaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    Optional<PayTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // ── 추천 API 용: 가게별 결제(방문) 횟수 ──

    @Query("select p.storeId, count(p) from PayTransaction p where p.status = 'SUCCESS' group by p.storeId")
    List<Object[]> countVisitsByStore();
}
