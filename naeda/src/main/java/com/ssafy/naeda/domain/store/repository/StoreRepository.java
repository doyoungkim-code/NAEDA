package com.ssafy.naeda.domain.store.repository;

import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select s
            from Store s
            where s.sourceType = :sourceType
              and s.isActive = true
              and (
                    s.imageUrl is null
                    or s.description is null
                    or s.rating is null
                    or s.rating = 0
                  )
            order by s.storeId asc
            """)
    List<Store> findIncompleteStoresForEnrichment(
            @Param("sourceType") StoreSourceType sourceType,
            Pageable pageable
    );
}
