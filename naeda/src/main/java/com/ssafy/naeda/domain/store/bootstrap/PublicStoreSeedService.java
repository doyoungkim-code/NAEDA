package com.ssafy.naeda.domain.store.bootstrap;

import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSeedMetadata;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.store.repository.StoreSeedMetadataRepository;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicStoreSeedService {

    private static final String SEED_KEY = "public-gumi-store-csv-v1";

    private final PublicStoreCsvLoader csvLoader;
    private final PublicStoreCoordinateConverter coordinateConverter;
    private final StoreRepository storeRepository;
    private final StoreSeedMetadataRepository storeSeedMetadataRepository;

    @Transactional
    public SeedSummary syncIfNeeded() {
        String contentHash = csvLoader.calculateContentHash();
        Optional<StoreSeedMetadata> metadata = storeSeedMetadataRepository.findById(SEED_KEY);
        if (metadata.isPresent() && contentHash.equals(metadata.get().getContentHash())) {
            log.info("[StoreSeed] CSV 변경 없음, 적재를 건너뜁니다.");
            return new SeedSummary(0, 0, 0, true);
        }

        List<PublicStoreCsvRecord> records = csvLoader.loadActiveStores();
        List<Store> existingStores = storeRepository.findBySourceType(StoreSourceType.PUBLIC_CSV);
        Map<String, Store> existingBySourceKey = new HashMap<>();
        Map<Long, String> existingSourceKeysById = new HashMap<>();
        for (Store store : existingStores) {
            if (store.getSourceKey() != null) {
                existingBySourceKey.put(store.getSourceKey(), store);
            }
            existingSourceKeysById.put(store.getStoreId(), store.getSourceKey());
        }

        List<Store> newStores = new ArrayList<>();
        Set<String> activeSourceKeys = new HashSet<>();
        int created = 0;
        int updated = 0;

        for (PublicStoreCsvRecord record : records) {
            activeSourceKeys.add(record.sourceKey());
            Store store = existingBySourceKey.get(record.sourceKey());
            PublicStoreCoordinateConverter.LatLng latLng = coordinateConverter.convert(record.x(), record.y());

            if (store == null) {
                Long storeId = generateStoreId(record.sourceKey(), existingSourceKeysById);
                Store newStore = Store.builder()
                        .storeId(storeId)
                        .storeName(record.storeName())
                        .categoryId(record.categoryId())
                        .categoryName(record.categoryName())
                        .roadAddress(record.roadAddress())
                        .numberAddress(record.numberAddress())
                        .latitude(latLng == null ? null : latLng.latitude())
                        .longitude(latLng == null ? null : latLng.longitude())
                        .phone(record.phone())
                        .isLocalBusiness(true)
                        .facePayEnabled(false)
                        .rating(0.0)
                        .sourceType(StoreSourceType.PUBLIC_CSV)
                        .sourceKey(record.sourceKey())
                        .isActive(true)
                        .build();
                newStores.add(newStore);
                existingSourceKeysById.put(storeId, record.sourceKey());
                created++;
                continue;
            }

            store.assignCatalogIdentity(StoreSourceType.PUBLIC_CSV, record.sourceKey());
            store.updatePublicCatalog(
                    record.storeName(),
                    record.categoryId(),
                    record.categoryName(),
                    record.roadAddress(),
                    record.numberAddress(),
                    latLng == null ? null : latLng.latitude(),
                    latLng == null ? null : latLng.longitude(),
                    record.phone()
            );
            updated++;
        }

        int deactivated = 0;
        for (Store existingStore : existingStores) {
            if (!activeSourceKeys.contains(existingStore.getSourceKey()) && Boolean.TRUE.equals(existingStore.getIsActive())) {
                existingStore.deactivate();
                deactivated++;
            }
        }

        if (!newStores.isEmpty()) {
            storeRepository.saveAll(newStores);
        }

        StoreSeedMetadata seedMetadata = metadata.orElseGet(() -> StoreSeedMetadata.builder()
                .seedKey(SEED_KEY)
                .contentHash(contentHash)
                .updated(LocalDateTime.now())
                .build());
        seedMetadata.updateHash(contentHash, LocalDateTime.now());
        storeSeedMetadataRepository.save(seedMetadata);

        log.info("[StoreSeed] 공공 매장 적재 완료 created={}, updated={}, deactivated={}", created, updated, deactivated);
        return new SeedSummary(created, updated, deactivated, false);
    }

    private Long generateStoreId(String sourceKey, Map<Long, String> existingSourceKeysById) {
        byte[] digest = sha256(sourceKey);
        for (int offset = 0; offset <= digest.length - Long.BYTES; offset += Long.BYTES) {
            long candidate = ByteBuffer.wrap(digest, offset, Long.BYTES).getLong();
            if (candidate == Long.MIN_VALUE) {
                candidate = Long.MAX_VALUE;
            }
            candidate = -Math.abs(candidate);
            if (candidate == 0L) {
                candidate = -1L;
            }
            String existingSourceKey = existingSourceKeysById.get(candidate);
            if (existingSourceKey == null || existingSourceKey.equals(sourceKey)) {
                return candidate;
            }
        }
        throw new IllegalStateException("store_id 해시 충돌을 해결하지 못했습니다: " + sourceKey);
    }

    private byte[] sha256(String sourceKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(sourceKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시를 생성할 수 없습니다.", e);
        }
    }

    public record SeedSummary(int created, int updated, int deactivated, boolean skipped) {
    }
}
