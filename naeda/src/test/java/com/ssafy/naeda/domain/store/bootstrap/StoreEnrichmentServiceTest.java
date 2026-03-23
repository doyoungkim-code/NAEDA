package com.ssafy.naeda.domain.store.bootstrap;

import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.domain.store.repository.StoreSeedMetadataRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreEnrichmentServiceTest {

    @InjectMocks
    private StoreEnrichmentService storeEnrichmentService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreSeedMetadataRepository storeSeedMetadataRepository;

    @Mock
    private NaverStoreEnrichmentClient naverStoreEnrichmentClient;

    @Test
    @DisplayName("누락 필드가 있는 공공 매장은 재보강 대상에 포함된다")
    void retryIncompleteStores_retriesMissingFields() {
        ReflectionTestUtils.setField(storeEnrichmentService, "enrichmentEnabled", true);

        Store store = Store.builder()
                .storeId(1L)
                .storeName("백운한정식")
                .categoryId("PUBLIC_RESTAURANT")
                .categoryName("한식")
                .sourceType(StoreSourceType.PUBLIC_CSV)
                .isActive(true)
                .rating(0.0)
                .build();

        given(storeRepository.findIncompleteStoresForEnrichment(any(StoreSourceType.class), any(Pageable.class)))
                .willReturn(List.of(store));
        given(naverStoreEnrichmentClient.enrich(store))
                .willReturn(Optional.of(new StoreEnrichmentData(
                        "https://example.com/store.jpg",
                        "한식당",
                        null
                )));

        int result = storeEnrichmentService.retryIncompleteStores();

        assertThat(result).isEqualTo(1);
        assertThat(store.getImageUrl()).isEqualTo("https://example.com/store.jpg");
        assertThat(store.getDescription()).isEqualTo("한식당");
        assertThat(store.getRating()).isEqualTo(0.0);
        verify(storeRepository).save(store);
    }

    @Test
    @DisplayName("빈 보강 결과가 와도 기존 이미지와 설명은 유지된다")
    void retryIncompleteStores_preservesExistingValuesWhenEmpty() {
        ReflectionTestUtils.setField(storeEnrichmentService, "enrichmentEnabled", true);

        Store store = Store.builder()
                .storeId(2L)
                .storeName("테스트 매장")
                .categoryId("PUBLIC_BAKERY")
                .categoryName("제과점영업")
                .sourceType(StoreSourceType.PUBLIC_CSV)
                .isActive(true)
                .imageUrl("https://example.com/original.jpg")
                .description("기존 설명")
                .rating(4.1)
                .build();

        given(storeRepository.findIncompleteStoresForEnrichment(any(StoreSourceType.class), any(Pageable.class)))
                .willReturn(List.of(store));
        given(naverStoreEnrichmentClient.enrich(store)).willReturn(Optional.empty());

        int result = storeEnrichmentService.retryIncompleteStores();

        assertThat(result).isEqualTo(1);
        assertThat(store.getImageUrl()).isEqualTo("https://example.com/original.jpg");
        assertThat(store.getDescription()).isEqualTo("기존 설명");
        assertThat(store.getRating()).isEqualTo(4.1);
        verify(storeRepository).save(store);
    }

    @Test
    @DisplayName("기본 이미지가 저장된 공공 매장은 실제 이미지로 재보강할 수 있다")
    void retryIncompleteStores_replacesDefaultImage() {
        ReflectionTestUtils.setField(storeEnrichmentService, "enrichmentEnabled", true);

        Store store = Store.builder()
                .storeId(3L)
                .storeName("김치찜은못참지인동점")
                .categoryId("PUBLIC_RESTAURANT")
                .categoryName("한식")
                .sourceType(StoreSourceType.PUBLIC_CSV)
                .isActive(true)
                .imageUrl("/images/store/default-restaurant.svg")
                .description("기존 설명")
                .build();

        given(storeRepository.findIncompleteStoresForEnrichment(any(StoreSourceType.class), any(Pageable.class)))
                .willReturn(List.of(store));
        given(naverStoreEnrichmentClient.enrich(store))
                .willReturn(Optional.of(new StoreEnrichmentData(
                        "https://search.pstatic.net/common/?autoRotate=true&type=f640_380&src=https%3A%2F%2Fldb-phinf.pstatic.net%2Fimage.jpg",
                        "방문자리뷰 5,458",
                        4.5
                )));

        int result = storeEnrichmentService.retryIncompleteStores();

        assertThat(result).isEqualTo(1);
        assertThat(store.getImageUrl())
                .isEqualTo("https://search.pstatic.net/common/?autoRotate=true&type=f640_380&src=https%3A%2F%2Fldb-phinf.pstatic.net%2Fimage.jpg");
        assertThat(store.getDescription()).isEqualTo("방문자리뷰 5,458");
        assertThat(store.getRating()).isEqualTo(4.5);
        verify(storeRepository).save(store);
    }
}
