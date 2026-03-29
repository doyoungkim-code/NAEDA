package com.ssafy.naeda.domain.store.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "store.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class StoreCatalogBootstrapRunner implements ApplicationRunner {

    private final PublicStoreSeedService publicStoreSeedService;
    private final StoreEnrichmentService storeEnrichmentService;

    @Override
    public void run(ApplicationArguments args) {
        PublicStoreSeedService.SeedSummary summary = publicStoreSeedService.syncIfNeeded();
        int refreshed = storeEnrichmentService.refreshActiveStoresIfNeeded();
        int enriched = storeEnrichmentService.enrichPendingStores();
        int retried = storeEnrichmentService.retryIncompleteStores();
        log.info("[StoreBootstrap] seedSkipped={}, created={}, updated={}, deactivated={}, refreshed={}, enriched={}, retried={}",
                summary.skipped(), summary.created(), summary.updated(), summary.deactivated(), refreshed, enriched, retried);
    }
}
