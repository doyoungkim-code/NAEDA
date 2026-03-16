package com.ssafy.naeda.domain.store.bootstrap;

public record StoreEnrichmentData(
        String imageUrl,
        String description,
        Double rating
) {
    public static StoreEnrichmentData empty() {
        return new StoreEnrichmentData(null, null, null);
    }
}
