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
                  )
            order by s.storeId asc
            """)
    List<Store> findIncompleteStoresForEnrichment(
            @Param("sourceType") StoreSourceType sourceType,
            Pageable pageable
    );

    // ── 추천 API 용 쿼리 ──

    /** 활성 가게가 존재하는 동 목록 (roadAddress에서 추출) */
    @Query("""
            select distinct substring(s.roadAddress, 1,
                   locate(' ', s.roadAddress,
                          locate(' ', s.roadAddress,
                                 locate(' ', s.roadAddress) + 1) + 1) - 1)
            from Store s
            where s.isActive = true
              and s.roadAddress is not null
            order by 1
            """)
    List<String> findDistinctDongs();

    /** 동 + 카테고리 필터 조합 (null이면 무시) */
    @Query("""
            select s
            from Store s
            where s.isActive = true
              and (:dong is null or s.roadAddress like concat('%', :dong, '%'))
              and (:categoryName is null or s.categoryName = :categoryName)
            """)
    List<Store> findByFilters(
            @Param("dong") String dong,
            @Param("categoryName") String categoryName
    );
}
