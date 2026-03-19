package com.ssafy.naeda.domain.store.bootstrap;

import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSeedMetadata;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.store.repository.StoreSeedMetadataRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreEnrichmentService {

    private static final String ENRICHMENT_KEY_PREFIX = "public-gumi-store-enrichment-";

    private final StoreRepository storeRepository;
    private final StoreSeedMetadataRepository storeSeedMetadataRepository;
    private final NaverStoreEnrichmentClient naverStoreEnrichmentClient;

    @Value("${store.enrichment.enabled:true}")
    private boolean enrichmentEnabled;

    @Value("${store.enrichment.version:map-v1}")
    private String enrichmentVersion;

    public int refreshActiveStoresIfNeeded() {
        if (!canEnrich()) {
            return 0;
        }

        String seedKey = ENRICHMENT_KEY_PREFIX + enrichmentVersion;
        if (storeSeedMetadataRepository.findById(seedKey).isPresent()) {
            log.info("[StoreEnrichment] 활성 매장 전체 재보강은 이미 완료되어 건너뜁니다. key={}", seedKey);
            return 0;
        }

        int refreshed = 0;
        int page = 0;
        while (true) {
            List<Store> batch = storeRepository.findBySourceTypeAndIsActiveTrueOrderByStoreIdAsc(
                    StoreSourceType.PUBLIC_CSV,
                    PageRequest.of(page, 100)
            );
            if (batch.isEmpty()) {
                break;
            }
            refreshed += enrichBatch(batch);
            page++;
        }

        storeSeedMetadataRepository.save(StoreSeedMetadata.builder()
                .seedKey(seedKey)
                .contentHash(enrichmentVersion)
                .updated(LocalDateTime.now())
                .build());

        log.info("[StoreEnrichment] 활성 공공 매장 전체 재보강 완료 count={}, key={}", refreshed, seedKey);
        return refreshed;
    }

    public int enrichPendingStores() {
        if (!canEnrich()) {
            return 0;
        }

        int enriched = 0;
        while (true) {
            List<Store> batch = storeRepository
                    .findTop100BySourceTypeAndIsActiveTrueAndLastEnrichedAtIsNullOrderByStoreIdAsc(
                            StoreSourceType.PUBLIC_CSV
                    );
            if (batch.isEmpty()) {
                return enriched;
            }

            enriched += enrichBatch(batch);
            log.info("[StoreEnrichment] 공공 매장 보강 처리 batchSize={}, totalProcessed={}", batch.size(), enriched);
        }
    }

    public int retryIncompleteStores() {
        if (!canEnrich()) {
            return 0;
        }

        List<Store> batch = storeRepository.findIncompleteStoresForEnrichment(
                StoreSourceType.PUBLIC_CSV,
                PageRequest.of(0, 100)
        );
        if (batch.isEmpty()) {
            return 0;
        }

        int retried = enrichBatch(batch);
        log.info("[StoreEnrichment] 누락 필드 재보강 처리 batchSize={}, retried={}", batch.size(), retried);
        return retried;
    }

    private boolean canEnrich() {
        if (!enrichmentEnabled) {
            log.info("[StoreEnrichment] 비활성화되어 있어 보강을 건너뜁니다.");
            return false;
        }
        return true;
    }

    private int enrichBatch(List<Store> stores) {
        int processed = 0;
        for (Store store : stores) {
            Optional<StoreEnrichmentData> enrichment = naverStoreEnrichmentClient.enrich(store);
            StoreEnrichmentData data = enrichment.orElse(StoreEnrichmentData.empty());
            store.updateEnrichment(
                    data.imageUrl(),
                    data.description(),
                    data.rating(),
                    LocalDateTime.now()
            );
            storeRepository.save(store);
            processed++;
        }
        return processed;
    }
}
