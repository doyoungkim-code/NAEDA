package com.ssafy.naeda.domain.store.bootstrap;

import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSeedMetadata;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.store.repository.StoreSeedMetadataRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicStoreSeedServiceTest {

    @InjectMocks
    private PublicStoreSeedService publicStoreSeedService;

    @Mock
    private PublicStoreCsvLoader csvLoader;

    @Mock
    private PublicStoreCoordinateConverter coordinateConverter;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreSeedMetadataRepository storeSeedMetadataRepository;

    @Test
    @DisplayName("CSV 해시가 같으면 시드를 건너뛴다")
    void syncIfNeeded_skipWhenHashMatches() {
        given(csvLoader.calculateContentHash()).willReturn("same-hash");
        given(storeSeedMetadataRepository.findById("public-gumi-store-csv-v1"))
                .willReturn(Optional.of(StoreSeedMetadata.builder()
                        .seedKey("public-gumi-store-csv-v1")
                        .contentHash("same-hash")
                        .updated(LocalDateTime.now())
                        .build()));

        PublicStoreSeedService.SeedSummary result = publicStoreSeedService.syncIfNeeded();

        assertThat(result.skipped()).isTrue();
        verify(storeRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("영업 중인 공공 매장을 신규 저장한다")
    void syncIfNeeded_createPublicStores() {
        PublicStoreCsvRecord record = new PublicStoreCsvRecord(
                "5080000-101-2018-00556",
                "백운한정식",
                "PUBLIC_RESTAURANT",
                "한식",
                "경상북도 구미시 인동35길 38",
                "경상북도 구미시 구평동 551-15",
                "0544758889",
                329195.302184417,
                289446.312403302
        );
        given(csvLoader.calculateContentHash()).willReturn("new-hash");
        given(storeSeedMetadataRepository.findById("public-gumi-store-csv-v1")).willReturn(Optional.empty());
        given(csvLoader.loadActiveStores()).willReturn(List.of(record));
        given(storeRepository.findBySourceType(StoreSourceType.PUBLIC_CSV)).willReturn(List.of());
        given(coordinateConverter.convert(record.x(), record.y()))
                .willReturn(new PublicStoreCoordinateConverter.LatLng(36.123456, 128.123456));

        publicStoreSeedService.syncIfNeeded();

        ArgumentCaptor<List<Store>> storesCaptor = ArgumentCaptor.forClass(List.class);
        verify(storeRepository).saveAll(storesCaptor.capture());
        List<Store> savedStores = storesCaptor.getValue();
        assertThat(savedStores).hasSize(1);
        Store saved = savedStores.get(0);
        assertThat(saved.getStoreName()).isEqualTo("백운한정식");
        assertThat(saved.getSourceType()).isEqualTo(StoreSourceType.PUBLIC_CSV);
        assertThat(saved.getSourceKey()).isEqualTo("5080000-101-2018-00556");
        assertThat(saved.getLatitude()).isEqualTo(36.123456);
        assertThat(saved.getLongitude()).isEqualTo(128.123456);
        assertThat(saved.getIsLocalBusiness()).isTrue();
        assertThat(saved.getFacePayEnabled()).isFalse();
    }
}
