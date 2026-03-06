package com.ssafy.naeda.domain.account.repository;

import com.ssafy.naeda.domain.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // 사용자의 계좌 목록 전체 조회
    List<Account> findByUserNo(Long userNo);

    // 계좌번호로 단건 조회
    Optional<Account> findByAccountNo(String accountNo);

    // 계좌번호 중복 확인 (계좌 등록 시)
    boolean existsByAccountNo(String accountNo);
}
