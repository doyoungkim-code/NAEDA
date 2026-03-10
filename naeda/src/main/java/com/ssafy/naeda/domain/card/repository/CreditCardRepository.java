package com.ssafy.naeda.domain.card.repository;

import com.ssafy.naeda.domain.card.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    // 카드번호로 활성 카드 단건 조회
    Optional<CreditCard> findByCardNoAndIsActiveTrue(String cardNo);

    // 카드 중복 확인
    boolean existsByCardNo(String cardNo);

    List<CreditCard> findByUserNoAndIsActiveTrue(Long userNo);
}
