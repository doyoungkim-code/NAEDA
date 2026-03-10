package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointOrderRepository extends JpaRepository<PointOrder, Long> {

    // 사용자별 주문 내역 조회
    List<PointOrder> findByUserNo(Long userNo);

    // 상품별 주문 수 조회
    Long countByProductId(Long productId);
}
