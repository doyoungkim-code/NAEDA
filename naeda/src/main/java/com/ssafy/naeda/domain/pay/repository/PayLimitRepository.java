package com.ssafy.naeda.domain.pay.repository;

import com.ssafy.naeda.domain.pay.entity.PayLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayLimitRepository extends JpaRepository<PayLimit, Long> {

    Optional<PayLimit> findByUserNo(Long userNo);

    void deleteByUserNo(Long userNo);
}
