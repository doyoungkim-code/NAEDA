package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointProduct;
import com.ssafy.naeda.domain.point.entity.PointProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointProductRepository extends JpaRepository<PointProduct, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pp FROM PointProduct pp WHERE pp.productId = :productId")
    Optional<PointProduct> findByIdForUpdate(@Param("productId") Long productId);

    // 상태별 조회 (ON_SALE 상품만 보기 등)
    List<PointProduct> findByStatus(PointProductStatus status);

    // 카테고리별 조회
    List<PointProduct> findByCategory(String category);

    // 카테고리 + 상태 조합 조회
    List<PointProduct> findByCategoryAndStatus(String category, PointProductStatus status);

    // 상품명 검색 (부분 일치)
    List<PointProduct> findByProductNameContaining(String keyword);

    // 상품명 검색 + 상태 조합
    List<PointProduct> findByProductNameContainingAndStatus(String keyword, PointProductStatus status);

    // 상품명 검색 + 카테고리 + 상태 조합
    List<PointProduct> findByProductNameContainingAndCategoryAndStatus(String keyword, String category, PointProductStatus status);
}
