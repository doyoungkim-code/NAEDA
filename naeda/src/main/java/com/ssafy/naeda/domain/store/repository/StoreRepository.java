package com.ssafy.naeda.domain.store.repository;

import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByUserNo(Long userNo);

    List<Store> findByCategoryId(String categoryId);

    List<Store> findByFacePayEnabledTrue();

    List<Store> findByCategoryIdAndFacePayEnabledTrue(String categoryId);

    Optional<Store> findBySsafyMerchantId(Long ssafyMerchantId);

    List<Store> findBySsafyMerchantIdIn(List<Long> ssafyMerchantIds);

    List<Store> findBySourceType(StoreSourceType sourceType);

    Optional<Store> findBySourceTypeAndSourceKey(StoreSourceType sourceType, String sourceKey);

    List<Store> findBySourceTypeAndIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByStoreNameAsc(
            StoreSourceType sourceType
    );

    List<Store> findTop100BySourceTypeAndIsActiveTrueAndLastEnrichedAtIsNullOrderByStoreIdAsc(
            StoreSourceType sourceType
    );
}
