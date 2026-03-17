package com.ssafy.naeda.domain.store.bootstrap;

public record PublicStoreCsvRecord(
        String sourceKey,
        String storeName,
        String categoryId,
        String categoryName,
        String roadAddress,
        String numberAddress,
        String phone,
        Double x,
        Double y
) {
}
