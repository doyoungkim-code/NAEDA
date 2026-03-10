package com.ssafy.naeda.domain.card.repository;

import com.ssafy.naeda.domain.card.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    // 사용자의 카드 목록 전체 조회
    List<CreditCard> findByUserNo(Long userNo);

    // 카드번호로 단건 조회
    Optional<CreditCard> findByCardNo(String cardNo);

    // 카드 중복 확인
    boolean existsByCardNo(String cardNo);
}
