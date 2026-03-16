package com.ssafy.naeda.domain.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.dto.request.StoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.response.StoreResponse;
import com.ssafy.naeda.domain.store.dto.ssafy.SsafyMerchantRec;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.entity.StoreSourceType;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.ssafy.SsafyApiClient;
import com.ssafy.naeda.global.ssafy.SsafyHeaderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private static final String MERCHANT_LIST_API  = "/edu/creditCard/inquireMerchantList";
    private static final String CREATE_MERCHANT_API = "/edu/creditCard/createMerchant";

    private final StoreRepository storeRepository;
    private final SsafyApiClient ssafyApiClient;
    private final SsafyHeaderFactory ssafyHeaderFactory;
    private final ObjectMapper objectMapper;

    /**
     * BE-036 매장 목록 조회.
     *
     * 흐름:
     * 1. SSAFY inquireMerchantList 호출 → 등록된 전체 가맹점 목록
     * 2. merchantId 기준으로 우리 DB Store와 병합
     *    - DB에 있는 경우: 상세 데이터(위치, facePayEnabled 등) 포함
     *    - DB에 없는 경우: SSAFY 기본 정보만 반환
     * 3. category / facePayOnly 필터 적용
     */
    public List<StoreResponse> getStores(String category, Boolean facePayOnly) {
        // 1. SSAFY 가맹점 목록 조회
        Map<String, Object> header = ssafyHeaderFactory.create("inquireMerchantList");
        Map<String, Object> body   = ssafyApiClient.buildBody(header);
        Map<String, Object> response = ssafyApiClient.post(MERCHANT_LIST_API, body);

        List<SsafyMerchantRec> ssafyMerchants = parseMerchantList(response);

        // 2. SSAFY merchantId → 우리 DB Map (storeId 기준)
        List<Long> merchantIds = ssafyMerchants.stream()
                .map(rec -> parseLong(rec.getMerchantId()))
                .filter(id -> id != null)
                .toList();

        Map<Long, Store> dbStoreMap = storeRepository.findAllById(merchantIds)
                .stream()
                .collect(Collectors.toMap(Store::getStoreId, s -> s));

        // 3. 병합 후 필터 적용
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

    /**
     * 지도 표시용 공공 매장 목록 조회.
     */
    public List<StoreResponse> getMapStores() {
        return storeRepository
                .findBySourceTypeAndIsActiveTrueAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByStoreNameAsc(
                        StoreSourceType.PUBLIC_CSV
                )
                .stream()
                .map(StoreResponse::from)
                .toList();
    }

    /**
     * BE-036 매장 단건 조회.
     */
    public StoreResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 매장입니다."));
        return StoreResponse.from(store);
    }

    /**
     * 매장 등록.
     *
     * 흐름:
     * 1. SSAFY createMerchant 호출 → merchantId 발급
     * 2. merchantId를 storeId로 사용하여 DB에 저장
     */
    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        // 1. SSAFY에 가맹점 등록
        Map<String, Object> header = ssafyHeaderFactory.create("createMerchant");
        Map<String, Object> body   = ssafyApiClient.buildBody(header,
                "categoryId",   request.getCategoryId(),
                "merchantName", request.getStoreName()
        );
        Map<String, Object> response = ssafyApiClient.post(CREATE_MERCHANT_API, body);

        // 2. 응답에서 신규 등록된 가맹점 추출 (merchantName 매칭)
        List<SsafyMerchantRec> merchants = parseMerchantList(response);
        SsafyMerchantRec created = merchants.stream()
                .filter(rec -> Objects.equals(request.getStoreName(), rec.getMerchantName()))
                .reduce((first, second) -> second)  // 동명 가맹점이 있을 경우 마지막(최신) 항목
                .orElseThrow(() -> new NotFoundException("SSAFY 가맹점 등록 응답에서 매장을 찾을 수 없습니다."));

        Long storeId = parseLong(created.getMerchantId());

        // 3. DB 저장
        Store store = Store.builder()
                .storeId(storeId)
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
                .sourceType(StoreSourceType.SSAFY)
                .sourceKey(storeId == null ? null : "ssafy:" + storeId)
                .build();

        return StoreResponse.from(storeRepository.save(store));
    }

    // ── 파싱 헬퍼 ──────────────────────────────────────────────────────────

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
