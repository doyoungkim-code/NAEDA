package com.ssafy.naeda.domain.festival.repository;

import com.ssafy.naeda.domain.festival.entity.Festival;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    List<Festival> findAllByOrderByStartDateDesc();
}
