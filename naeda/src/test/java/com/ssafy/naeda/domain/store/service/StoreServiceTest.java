package com.ssafy.naeda.domain.store.service;

import com.ssafy.naeda.domain.store.dto.request.StoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.response.StoreResponse;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private SsafyApiClient ssafyApiClient;

    @Mock
    private SsafyHeaderFactory ssafyHeaderFactory;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private List<Map<String, Object>> ssafyRec;

    @BeforeEach
    void setUp() {
        ssafyRec = List.of(
                Map.of("categoryId", "CG-9ca85f66311a23d", "categoryName", "생활",
                        "merchantId", "1", "merchantName", "스타벅스"),
                Map.of("categoryId", "CG-4fa85f6425ad1d3", "categoryName", "대형마트",
                        "merchantId", "2", "merchantName", "코스트코"),
                Map.of("categoryId", "CG-4fa85f6425ad1d3", "categoryName", "대형마트",
                        "merchantId", "3", "merchantName", "홈플러스")
        );
    }

    private void givenSsafyMerchantList() {
        given(ssafyHeaderFactory.create("inquireMerchantList")).willReturn(Map.of());
        given(ssafyApiClient.buildBody(any())).willReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/creditCard/inquireMerchantList"), any()))
                .willReturn(Map.of("REC", ssafyRec));
    }

    // ── getStores ────────────────────────────────────────────────────────

    @Test
    @DisplayName("매장 목록 조회 - SSAFY 가맹점과 DB 데이터 병합")
    void getStores_mergesSsafyAndDb() {
        givenSsafyMerchantList();

        Store dbStore = Store.builder()
                .storeId(1L).userNo(10L).storeName("스타벅스 구미점")
                .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                .roadAddress("경북 구미시 대학로 1")
                .facePayEnabled(true).isLocalBusiness(false).rating(4.2)
                .build();
        given(storeRepository.findAllById(List.of(1L, 2L, 3L))).willReturn(List.of(dbStore));

        List<StoreResponse> result = storeService.getStores(null, null);

        assertThat(result).hasSize(3);
        // storeId=1: DB 데이터 우선 (상세 정보 + categoryId/categoryName 포함)
        StoreResponse store1 = result.stream().filter(s -> s.getStoreId().equals(1L)).findFirst().orElseThrow();
        assertThat(store1.getRoadAddress()).isEqualTo("경북 구미시 대학로 1");
        assertThat(store1.getCategoryId()).isEqualTo("CG-9ca85f66311a23d");
        assertThat(store1.getCategoryName()).isEqualTo("생활");
        // storeId=2: SSAFY-only (roadAddress null, categoryId/categoryName SSAFY 기준)
        StoreResponse store2 = result.stream().filter(s -> s.getStoreId().equals(2L)).findFirst().orElseThrow();
        assertThat(store2.getStoreName()).isEqualTo("코스트코");
        assertThat(store2.getRoadAddress()).isNull();
        assertThat(store2.getCategoryId()).isEqualTo("CG-4fa85f6425ad1d3");
        assertThat(store2.getCategoryName()).isEqualTo("대형마트");
    }

    @Test
    @DisplayName("매장 목록 조회 - category 필터 적용")
    void getStores_categoryFilter() {
        givenSsafyMerchantList();
        given(storeRepository.findAllById(any())).willReturn(List.of());

        List<StoreResponse> result = storeService.getStores("CG-4fa85f6425ad1d3", null);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> "CG-4fa85f6425ad1d3".equals(s.getCategoryId()));
    }

    @Test
    @DisplayName("매장 목록 조회 - facePayOnly 필터: DB에 있고 facePayEnabled=true인 항목만 통과")
    void getStores_facePayOnlyFilter() {
        givenSsafyMerchantList();

        Store facePayStore = Store.builder()
                .storeId(1L).userNo(10L).storeName("스타벅스 구미점")
                .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                .roadAddress("경북 구미시 대학로 1")
                .facePayEnabled(true).isLocalBusiness(false).rating(4.2)
                .build();
        given(storeRepository.findAllById(any())).willReturn(List.of(facePayStore));

        List<StoreResponse> result = storeService.getStores(null, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFacePayEnabled()).isTrue();
    }

    @Test
    @DisplayName("지도용 매장 목록 조회 - 공공 CSV 매장만 반환")
    void getMapStores_publicOnly() {
        Store publicStore = Store.builder()
                .storeId(-10L)
                .storeName("백운한정식")
                .categoryId("PUBLIC_RESTAURANT")
                .categoryName("한식")
                .roadAddress("경상북도 구미시 인동35길 38")
                .latitude(36.12)
                .longitude(128.34)
                .sourceType(StoreSourceType.PUBLIC_CSV)
                .isActive(true)
                .build();
        given(storeRepository.findBySourceTypeAndIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByStoreNameAsc(
                StoreSourceType.PUBLIC_CSV
        )).willReturn(List.of(publicStore));

        List<StoreResponse> result = storeService.getMapStores();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStoreId()).isEqualTo(-10L);
        assertThat(result.get(0).getSourceType()).isEqualTo(StoreSourceType.PUBLIC_CSV);
    }

    // ── getStore ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("매장 단건 조회 - 정상 조회")
    void getStore_success() {
        Store store = Store.builder()
                .storeId(1L).userNo(10L).storeName("스타벅스 구미점")
                .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                .roadAddress("경북 구미시 대학로 1")
                .facePayEnabled(true).isLocalBusiness(false).rating(4.2)
                .build();
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        StoreResponse result = storeService.getStore(1L);

        assertThat(result.getStoreId()).isEqualTo(1L);
        assertThat(result.getStoreName()).isEqualTo("스타벅스 구미점");
    }

    @Test
    @DisplayName("매장 단건 조회 - 존재하지 않는 storeId면 예외")
    void getStore_notFound() {
        given(storeRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.getStore(999L))
                .isInstanceOf(NotFoundException.class);
    }

    // ── createStore ──────────────────────────────────────────────────────

    @Test
    @DisplayName("매장 등록 - SSAFY createMerchant 호출 후 merchantId를 storeId로 DB 저장")
    void createStore_success() {
        List<Map<String, Object>> createRec = List.of(
                Map.of("categoryId", "CG-9ca85f66311a23d", "categoryName", "생활",
                        "merchantId", "1", "merchantName", "스타벅스"),
                Map.of("categoryId", "CG-4fa85f6425ad1d3", "categoryName", "대형마트",
                        "merchantId", "3", "merchantName", "코스트코")
        );

        given(ssafyHeaderFactory.create("createMerchant")).willReturn(Map.of());
        given(ssafyApiClient.buildBody(any(), any(), any(), any(), any())).willReturn(Map.of());
        given(ssafyApiClient.post(eq("/edu/creditCard/createMerchant"), any()))
                .willReturn(Map.of("REC", createRec));

        Store savedStore = Store.builder()
                .storeId(3L).userNo(10L).storeName("코스트코")
                .categoryId("CG-4fa85f6425ad1d3").categoryName("대형마트")
                .roadAddress("경북 구미시 산호대로 1")
                .facePayEnabled(true).isLocalBusiness(false).rating(0.0)
                .build();
        given(storeRepository.save(any(Store.class))).willReturn(savedStore);

        StoreCreateRequest request = new StoreCreateRequest();
        ReflectionTestUtils.setField(request, "storeName", "코스트코");

        StoreResponse result = storeService.createStore(request);

        assertThat(result.getStoreId()).isEqualTo(3L);
        assertThat(result.getStoreName()).isEqualTo("코스트코");
    }
}
