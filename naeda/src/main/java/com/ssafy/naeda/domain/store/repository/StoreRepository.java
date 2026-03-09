package com.ssafy.naeda.domain.store.repository;

import com.ssafy.naeda.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByUserNo(Long userNo);

    List<Store> findByCategory(String category);

    List<Store> findByFacePayEnabledTrue();

    List<Store> findByCategoryAndFacePayEnabledTrue(String category);
}
