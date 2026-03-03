package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}
