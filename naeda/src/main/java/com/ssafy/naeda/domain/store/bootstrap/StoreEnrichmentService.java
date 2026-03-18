package com.ssafy.naeda.domain.store.bootstrap;

import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
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

    private final StoreRepository storeRepository;
    private final NaverStoreEnrichmentClient naverStoreEnrichmentClient;

    @Value("${store.enrichment.enabled:true}")
    private boolean enrichmentEnabled;

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
