package com.ssafy.naeda.domain.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.bootstrap.NaverStoreEnrichmentClient;
import com.ssafy.naeda.domain.store.bootstrap.StoreEnrichmentData;
import com.ssafy.naeda.domain.store.dto.request.PublicStoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.request.StoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.response.StoreResponse;
import com.ssafy.naeda.domain.store.dto.ssafy.SsafyMerchantRec;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private static final String MERCHANT_LIST_API  = "/edu/creditCard/inquireMerchantList";
    private static final String CREATE_MERCHANT_API = "/edu/creditCard/createMerchant";
    private static final Set<String> PUBLIC_CATEGORY_IDS = Set.of("PUBLIC_RESTAURANT", "PUBLIC_BAKERY");

    private final StoreRepository storeRepository;
    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final ObjectMapper objectMapper;
    private final NaverStoreEnrichmentClient naverStoreEnrichmentClient;

    public List<StoreResponse> getStores(String category, Boolean facePayOnly) {
        Map<String, Object> header = ssafyHeaderFactory.create("inquireMerchantList");
        Map<String, Object> body   = ssafyApiClient.buildBody(header);
        Map<String, Object> response = ssafyApiClient.post(MERCHANT_LIST_API, body);

        List<SsafyMerchantRec> ssafyMerchants = parseMerchantList(response);

        List<Long> merchantIds = ssafyMerchants.stream()
                .map(rec -> parseLong(rec.getMerchantId()))
                .filter(id -> id != null)
                .toList();

        Map<Long, Store> dbStoreMap = storeRepository.findBySsafyMerchantIdIn(merchantIds)
                .stream()
                .filter(store -> store.resolveSsafyMerchantId() != null)
                .collect(Collectors.toMap(Store::resolveSsafyMerchantId, s -> s));

        return ssafyMerchants.stream()
                .map(rec -> {
                    Long id = parseLong(rec.getMerchantId());
                    Store store = (id != null) ? dbStoreMap.get(id) : null;
                    return store != null ? StoreResponse.from(store) : StoreResponse.fromSsafy(rec);
                })
                .filter(s -> category == null || category.equals(s.getCategoryId()))
                .filter(s -> facePayOnly == null || !facePayOnly || Boolean.TRUE.equals(s.getFacePayEnabled()))
                .toList();
    }

    public List<StoreResponse> getMapStores() {
        return storeRepository
                .findBySourceTypeAndIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByStoreNameAsc(
                        StoreSourceType.PUBLIC_CSV
                )
                .stream()
                .map(StoreResponse::from)
                .toList();
    }

    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));
        return StoreResponse.from(store);
    }

    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        Map<String, Object> header = ssafyHeaderFactory.create("createMerchant");
        Map<String, Object> body   = ssafyApiClient.buildBody(header,
                "categoryId",   request.getCategoryId(),
                "merchantName", request.getStoreName()
        );
        Map<String, Object> response = ssafyApiClient.post(CREATE_MERCHANT_API, body);

        List<SsafyMerchantRec> merchants = parseMerchantList(response);
        SsafyMerchantRec created = merchants.stream()
                .filter(rec -> Objects.equals(request.getStoreName(), rec.getMerchantName()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new NotFoundException("SSAFY 가맹점 등록 응답에서 매장을 찾을 수 없습니다."));

        Long ssafyMerchantId = parseLong(created.getMerchantId());

        Store store = Store.builder()
                .userNo(request.getUserNo())
                .accountId(request.getAccountId())
                .storeName(request.getStoreName())
                .categoryId(request.getCategoryId())
                .categoryName(created.getCategoryName())
                .roadAddress(request.getRoadAddress())
                .numberAddress(request.getNumberAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phone(request.getPhone())
                .isLocalBusiness(Boolean.TRUE.equals(request.getIsLocalBusiness()))
                .facePayEnabled(Boolean.TRUE.equals(request.getFacePayEnabled()))
                .build();
        store.assignSsafyIdentity(ssafyMerchantId);

        return StoreResponse.from(storeRepository.save(store));
    }

    @Transactional
    public StoreResponse createPublicStore(PublicStoreCreateRequest request) {
        validatePublicCategory(request.getCategoryId());

        Store store = Store.builder()
                .userNo(request.getUserNo())
                .accountId(request.getAccountId())
                .storeName(request.getStoreName())
                .categoryId(request.getCategoryId())
                .categoryName(resolvePublicCategoryName(request))
                .roadAddress(request.getRoadAddress())
                .numberAddress(request.getNumberAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phone(request.getPhone())
                .isLocalBusiness(request.getIsLocalBusiness() == null ? true : request.getIsLocalBusiness())
                .facePayEnabled(Boolean.TRUE.equals(request.getFacePayEnabled()))
                .rating(0.0)
                .sourceType(StoreSourceType.PUBLIC_CSV)
                .sourceKey("manual:" + UUID.randomUUID())
                .isActive(true)
                .build();

        Optional<StoreEnrichmentData> enrichment = naverStoreEnrichmentClient.enrich(store);
        if (enrichment.isPresent() && enrichment.get().hasAnyValue()) {
            StoreEnrichmentData data = enrichment.get();
            store.updateEnrichment(data.imageUrl(), data.description(), data.rating(), LocalDateTime.now());
        }

        return StoreResponse.from(storeRepository.save(store));
    }

    private void validatePublicCategory(String categoryId) {
        if (!PUBLIC_CATEGORY_IDS.contains(categoryId)) {
            throw new IllegalArgumentException("공공 매장 categoryId는 PUBLIC_RESTAURANT 또는 PUBLIC_BAKERY 여야 합니다.");
        }
    }

    private String resolvePublicCategoryName(PublicStoreCreateRequest request) {
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            return request.getCategoryName();
        }
        return "PUBLIC_BAKERY".equals(request.getCategoryId()) ? "제과점영업" : "한식";
    }

    private List<SsafyMerchantRec> parseMerchantList(Map<String, Object> response) {
        Object rec = response.get("REC");
        if (rec == null) return List.of();
        return objectMapper.convertValue(
                rec,
                objectMapper.getTypeFactory().constructCollectionType(List.class, SsafyMerchantRec.class)
        );
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("[StoreService] merchantId 파싱 실패: value={}", value);
            return null;
        }
    }
}
