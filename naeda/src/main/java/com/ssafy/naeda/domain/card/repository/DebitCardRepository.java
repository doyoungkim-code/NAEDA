package com.ssafy.naeda.domain.card.repository;

import com.ssafy.naeda.domain.card.entity.DebitCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DebitCardRepository extends JpaRepository<DebitCard, Long> {

    // 사용자의 체크카드 전체 조회
    List<DebitCard> findByUserNo(Long userNo);

    // 카드번호로 단건 조회
    Optional<DebitCard> findByCardNo(String cardNo);

    // 중복 체크
    boolean existsByCardNo(String cardNo);
}
