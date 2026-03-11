package com.ssafy.naeda.domain.fds.repository;

import com.ssafy.naeda.domain.fds.entity.FdsLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FdsLogRepository extends JpaRepository<FdsLog, Long> {

    Optional<FdsLog> findByPaymentId(Long paymentId);

    List<FdsLog> findByUserNo(Long userNo);
}
