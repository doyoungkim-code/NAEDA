package com.ssafy.naeda.domain.account.repository;

import com.ssafy.naeda.domain.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // 사용자의 계좌 목록 전체 조회
    List<Account> findByUserNo(Long userNo);

    // 계좌번호로 단건 조회
    Optional<Account> findByAccountNo(String accountNo);

    // 계좌번호로 조회 + 비관적 잠금 (이체 시 사용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNo = :accountNo")
    Optional<Account> findByAccountNoForUpdate(@Param("accountNo") String accountNo);

    // 계좌번호 중복 확인 (계좌 등록 시)
    boolean existsByAccountNo(String accountNo);
}
