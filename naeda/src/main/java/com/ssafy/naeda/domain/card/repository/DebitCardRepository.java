package com.ssafy.naeda.domain.card.repository;

import com.ssafy.naeda.domain.card.entity.DebitCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DebitCardRepository extends JpaRepository<DebitCard, Long> {

    // 카드번호로 활성 카드 단건 조회
    Optional<DebitCard> findByCardNoAndIsActiveTrue(String cardNo);

    // 중복 체크
    boolean existsByCardNo(String cardNo);

    List<DebitCard> findByUserNoAndIsActiveTrue(Long userNo);

    void deleteByUserNo(Long userNo);
}
