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
                .map(store -> StoreResponse.from(store, resolveMapFacePayEnabled(store)))
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

    /**
     * DB에 존재하지만 SSAFY 가맹점 등록이 안 된 매장들을 일괄 등록하고
     * 발급받은 merchantId를 매핑한다.
     *
     * 핵심: createMerchant 호출 전후 목록을 비교해서 새로 생긴 merchantId를 찾는다.
     */
    @Transactional
    public Map<String, Object> registerAllUnregisteredMerchants(int limit) {
        // ssafy_merchant_id가 null인 활성 매장 조회 (limit 개수만)
        List<Store> unregistered = storeRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> s.getSsafyMerchantId() == null)
                .limit(limit)
                .toList();

        if (unregistered.isEmpty()) {
            return Map.of("message", "등록할 매장이 없습니다.", "registered", 0);
        }

        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();

        // 현재 SSAFY에 등록된 전체 가맹점 목록의 merchantId 집합을 먼저 조회
        Set<String> knownMerchantIds = fetchCurrentMerchantIds();

        for (Store store : unregistered) {
            try {
                String ssafyCategoryId = mapToSsafyCategoryId(store.getCategoryId());

                // createMerchant 호출
                Map<String, Object> header = ssafyHeaderFactory.create("createMerchant");
                Map<String, Object> body = ssafyApiClient.buildBody(header,
                        "categoryId", ssafyCategoryId,
                        "merchantName", store.getStoreName()
                );
                Map<String, Object> response = ssafyApiClient.post(CREATE_MERCHANT_API, body);

                List<SsafyMerchantRec> merchants = parseMerchantList(response);

                // 호출 전에 없었던 새로운 merchantId 찾기
                SsafyMerchantRec newMerchant = merchants.stream()
                        .filter(rec -> rec.getMerchantId() != null && !knownMerchantIds.contains(rec.getMerchantId()))
                        .findFirst()
                        .orElse(null);

                if (newMerchant != null) {
                    Long merchantId = parseLong(newMerchant.getMerchantId());
                    store.assignSsafyIdentity(merchantId);
                    storeRepository.save(store);
                    // 새로 등록한 ID를 known 집합에 추가 (다음 루프에서 중복 방지)
                    knownMerchantIds.add(newMerchant.getMerchantId());
                    successCount++;
                    results.add(Map.of(
                            "storeId", store.getStoreId(),
                            "storeName", store.getStoreName(),
                            "merchantId", merchantId != null ? merchantId : "null",
                            "status", "SUCCESS"
                    ));
                    log.info("[StoreService] SSAFY 가맹점 등록 성공: storeId={}, storeName={}, merchantId={}",
                            store.getStoreId(), store.getStoreName(), merchantId);
                } else {
                    failCount++;
                    results.add(Map.of(
                            "storeId", store.getStoreId(),
                            "storeName", store.getStoreName(),
                            "status", "FAIL",
                            "reason", "새로운 merchantId를 찾을 수 없음"
                    ));
                    log.warn("[StoreService] SSAFY 가맹점 등록 실패: storeId={}, storeName={}",
                            store.getStoreId(), store.getStoreName());
                }
            } catch (Exception e) {
                failCount++;
                results.add(Map.of(
                        "storeId", store.getStoreId(),
                        "storeName", store.getStoreName(),
                        "status", "FAIL",
                        "reason", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류"
                ));
                log.error("[StoreService] SSAFY 가맹점 등록 중 오류: storeId={}, storeName={}, error={}",
                        store.getStoreId(), store.getStoreName(), e.getMessage());
            }
        }

        // 남은 미등록 매장 수 계산
        long remaining = storeRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> s.getSsafyMerchantId() == null)
                .count();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", unregistered.size());
        result.put("success", successCount);
        result.put("fail", failCount);
        result.put("remaining", remaining);
        result.put("details", results);
        return result;
    }

    /**
     * SSAFY에 현재 등록된 전체 가맹점 목록의 merchantId 집합을 조회한다.
     */
    private Set<String> fetchCurrentMerchantIds() {
        try {
            Map<String, Object> header = ssafyHeaderFactory.create("inquireMerchantList");
            Map<String, Object> body = ssafyApiClient.buildBody(header);
            Map<String, Object> response = ssafyApiClient.post(MERCHANT_LIST_API, body);

            return parseMerchantList(response).stream()
                    .map(SsafyMerchantRec::getMerchantId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(java.util.HashSet::new));
        } catch (Exception e) {
            log.warn("[StoreService] 기존 가맹점 목록 조회 실패, 빈 집합으로 시작: {}", e.getMessage());
            return new java.util.HashSet<>();
        }
    }

    /**
     * 공공 CSV 카테고리 → SSAFY 카테고리 매핑.
     * PUBLIC_RESTAURANT, PUBLIC_BAKERY 등은 SSAFY의 "생활" 카테고리로 매핑.
     * 이미 SSAFY 카테고리 ID(CG- 접두사)이면 그대로 사용.
     */
    private String mapToSsafyCategoryId(String categoryId) {
        if (categoryId == null) {
            return "CG-9ca85f66311a23d"; // 기본: 생활
        }
        if (categoryId.startsWith("CG-")) {
            return categoryId; // 이미 SSAFY 카테고리
        }
        // 공공 CSV 카테고리 → SSAFY "생활" 카테고리로 매핑
        return switch (categoryId) {
            case "PUBLIC_RESTAURANT" -> "CG-9ca85f66311a23d"; // 생활 (음식점, 커피전문점, 편의점, 약국..)
            case "PUBLIC_BAKERY"     -> "CG-9ca85f66311a23d"; // 생활
            default                  -> "CG-9ca85f66311a23d"; // 기본: 생활
        };
    }

    private boolean resolveMapFacePayEnabled(Store store) {
        if (!Boolean.TRUE.equals(store.getFacePayEnabled())) {
            return false;
        }

        String seed = firstNonBlank(
                store.getSourceKey(),
                store.getStoreName(),
                store.getRoadAddress(),
                store.getStoreId() == null ? null : String.valueOf(store.getStoreId())
        );
        if (seed == null) {
            return false;
        }

        return Math.floorMod(seed.hashCode(), 10) < 4;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
