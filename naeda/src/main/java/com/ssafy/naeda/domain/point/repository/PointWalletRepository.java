package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {

    Optional<PointWallet> findByUserNo(Long userNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pw FROM PointWallet pw WHERE pw.userNo = :userNo")
    Optional<PointWallet> findByUserNoForUpdate(@Param("userNo") Long userNo);
}
