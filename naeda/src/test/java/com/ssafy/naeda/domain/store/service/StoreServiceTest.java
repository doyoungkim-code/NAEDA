package com.ssafy.naeda.domain.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.bootstrap.NaverStoreEnrichmentClient;
import com.ssafy.naeda.domain.store.bootstrap.StoreEnrichmentData;
import com.ssafy.naeda.domain.store.dto.request.PublicStoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.request.StoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.response.StoreResponse;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

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

    @Mock
    private NaverStoreEnrichmentClient naverStoreEnrichmentClient;

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

    @Test
    @DisplayName("매장 목록 조회 - SSAFY 가맹점과 DB 데이터 병합")
    void getStores_mergesSsafyAndDb() {
        givenSsafyMerchantList();

        Store dbStore = Store.builder()
                .storeId(101L).ssafyMerchantId(1L).userNo(10L).storeName("스타벅스 구미점")
                .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                .roadAddress("경북 구미시 대학로 1")
                .facePayEnabled(true).isLocalBusiness(false).rating(4.2)
                .build();
        given(storeRepository.findBySsafyMerchantIdIn(List.of(1L, 2L, 3L))).willReturn(List.of(dbStore));

        List<StoreResponse> result = storeService.getStores(null, null);

        assertThat(result).hasSize(3);
        StoreResponse store1 = result.stream().filter(s -> s.getSsafyMerchantId().equals(1L)).findFirst().orElseThrow();
        assertThat(store1.getStoreId()).isEqualTo(101L);
        assertThat(store1.getRoadAddress()).isEqualTo("경북 구미시 대학로 1");
        assertThat(store1.getCategoryId()).isEqualTo("CG-9ca85f66311a23d");
        assertThat(store1.getCategoryName()).isEqualTo("생활");
        StoreResponse store2 = result.stream().filter(s -> s.getSsafyMerchantId().equals(2L)).findFirst().orElseThrow();
        assertThat(store2.getStoreId()).isNull();
        assertThat(store2.getStoreName()).isEqualTo("코스트코");
        assertThat(store2.getRoadAddress()).isNull();
        assertThat(store2.getCategoryId()).isEqualTo("CG-4fa85f6425ad1d3");
        assertThat(store2.getCategoryName()).isEqualTo("대형마트");
    }

    @Test
    @DisplayName("매장 목록 조회 - category 필터 적용")
    void getStores_categoryFilter() {
        givenSsafyMerchantList();
        given(storeRepository.findBySsafyMerchantIdIn(any())).willReturn(List.of());

        List<StoreResponse> result = storeService.getStores("CG-4fa85f6425ad1d3", null);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> "CG-4fa85f6425ad1d3".equals(s.getCategoryId()));
    }

    @Test
    @DisplayName("매장 목록 조회 - facePayOnly 필터: DB에 있고 facePayEnabled=true인 항목만 통과")
    void getStores_facePayOnlyFilter() {
        givenSsafyMerchantList();

        Store facePayStore = Store.builder()
                .storeId(101L).ssafyMerchantId(1L).userNo(10L).storeName("스타벅스 구미점")
                .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                .roadAddress("경북 구미시 대학로 1")
                .facePayEnabled(true).isLocalBusiness(false).rating(4.2)
                .build();
        given(storeRepository.findBySsafyMerchantIdIn(any())).willReturn(List.of(facePayStore));

        List<StoreResponse> result = storeService.getStores(null, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFacePayEnabled()).isTrue();
    }

    @Test
    @DisplayName("지도용 매장 목록 조회 - 공공 CSV 매장만 반환")
    void getMapStores_publicOnly() {
        Store publicStore = Store.builder()
                .storeId(10L)
                .storeName("백운한정식")
                .categoryId("PUBLIC_RESTAURANT")
                .categoryName("한식")
                .roadAddress("경상북도 구미시 인동35길 38")
                .latitude(36.12)
                .longitude(128.34)
                .sourceKey("public:10")
                .facePayEnabled(true)
                .sourceType(StoreSourceType.PUBLIC_CSV)
                .isActive(true)
                .build();
        Store nonFacePayStore = Store.builder()
                .storeId(11L)
                .storeName("해평빵집")
                .categoryId("PUBLIC_BAKERY")
                .categoryName("제과점영업")
                .roadAddress("경상북도 구미시 해평면 11")
                .latitude(36.11)
                .longitude(128.31)
                .sourceKey("public:11")
                .facePayEnabled(false)
                .sourceType(StoreSourceType.PUBLIC_CSV)
                .isActive(true)
                .build();
        given(storeRepository.findBySourceTypeAndIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByStoreNameAsc(
                StoreSourceType.PUBLIC_CSV
        )).willReturn(List.of(publicStore, nonFacePayStore));

        List<StoreResponse> result = storeService.getMapStores();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(store -> store.getSourceType() == StoreSourceType.PUBLIC_CSV);
        StoreResponse first = result.stream()
                .filter(store -> store.getStoreId().equals(10L))
                .findFirst()
                .orElseThrow();
        StoreResponse second = result.stream()
                .filter(store -> store.getStoreId().equals(11L))
                .findFirst()
                .orElseThrow();
        assertThat(first.getFacePayEnabled())
                .isEqualTo(Math.floorMod("public:10".hashCode(), 10) < 4);
        assertThat(second.getFacePayEnabled()).isFalse();
    }

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

    @Test
    @DisplayName("매장 등록 - 내부 storeId와 별도 ssafyMerchantId를 저장한다")
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
                .storeId(30L).ssafyMerchantId(3L).userNo(10L).storeName("코스트코")
                .categoryId("CG-4fa85f6425ad1d3").categoryName("대형마트")
                .roadAddress("경북 구미시 산호대로 1")
                .facePayEnabled(true).isLocalBusiness(false).rating(0.0)
                .build();
        given(storeRepository.save(any(Store.class))).willReturn(savedStore);

        StoreCreateRequest request = new StoreCreateRequest();
        ReflectionTestUtils.setField(request, "storeName", "코스트코");

        StoreResponse result = storeService.createStore(request);

        assertThat(result.getStoreId()).isEqualTo(30L);
        assertThat(result.getSsafyMerchantId()).isEqualTo(3L);
        assertThat(result.getStoreName()).isEqualTo("코스트코");
    }

    @Test
    @DisplayName("공공 매장 수동 등록 - PUBLIC_RESTAURANT 요청을 저장한다")
    void createPublicStore_success() {
        PublicStoreCreateRequest request = new PublicStoreCreateRequest();
        ReflectionTestUtils.setField(request, "categoryId", "PUBLIC_RESTAURANT");
        ReflectionTestUtils.setField(request, "categoryName", "한식");
        ReflectionTestUtils.setField(request, "storeName", "TEST");
        ReflectionTestUtils.setField(request, "userNo", 14L);
        ReflectionTestUtils.setField(request, "accountId", 14L);
        ReflectionTestUtils.setField(request, "roadAddress", "경상북도 구미시 해평면 도리사로 403-1, C동 1,2층");
        ReflectionTestUtils.setField(request, "numberAddress", "경상북도 구미시 해평면 송곡리 398-6 1,2층 C동");
        ReflectionTestUtils.setField(request, "latitude", 36.110307);
        ReflectionTestUtils.setField(request, "longitude", 128.411495);
        ReflectionTestUtils.setField(request, "phone", "01051918793");
        ReflectionTestUtils.setField(request, "isLocalBusiness", true);
        ReflectionTestUtils.setField(request, "facePayEnabled", true);

        given(naverStoreEnrichmentClient.enrich(any(Store.class)))
                .willReturn(Optional.of(new StoreEnrichmentData(
                        "https://example.com/store.jpg",
                        "테스트 소개",
                        4.2
                )));

        Store savedStore = Store.builder()
                .storeId(999L).userNo(14L).accountId(14L).storeName("TEST")
                .categoryId("PUBLIC_RESTAURANT").categoryName("한식")
                .roadAddress("경상북도 구미시 해평면 도리사로 403-1, C동 1,2층")
                .numberAddress("경상북도 구미시 해평면 송곡리 398-6 1,2층 C동")
                .latitude(36.110307).longitude(128.411495)
                .phone("01051918793")
                .isLocalBusiness(true).facePayEnabled(true)
                .sourceType(StoreSourceType.PUBLIC_CSV).isActive(true)
                .imageUrl("https://example.com/store.jpg").description("테스트 소개")
                .rating(4.2)
                .build();
        given(storeRepository.save(any(Store.class))).willReturn(savedStore);

        StoreResponse result = storeService.createPublicStore(request);

        assertThat(result.getStoreId()).isEqualTo(999L);
        assertThat(result.getCategoryId()).isEqualTo("PUBLIC_RESTAURANT");
        assertThat(result.getSourceType()).isEqualTo(StoreSourceType.PUBLIC_CSV);
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/store.jpg");
    }
}
