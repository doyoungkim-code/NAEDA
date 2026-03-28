package com.ssafy.naeda.domain.recommend.service;

import com.ssafy.naeda.domain.pay.repository.PayTransactionRepository;
import com.ssafy.naeda.domain.recommend.dto.response.RecommendResponse;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendService {

    private static final double RATING_WEIGHT = 0.6;
    private static final double VISIT_WEIGHT = 0.4;

    private final StoreRepository storeRepository;
    private final PayTransactionRepository payTransactionRepository;

    /**
     * 추천 가게 목록 조회.
     *
     * @param dong     동 이름 (nullable - null이면 구미 전체)
     * @param category 카테고리명 (nullable)
     * @param sort     정렬 기준: "rating", "visits", null(기본=추천점수)
     * @return 추천 목록
     */
    public List<RecommendResponse> getRecommendStores(String dong, String category, String sort) {
        // 1. dong에서 면/읍/동 접미사 제거하여 유연한 검색 (산동면 → 산동, 산동읍 모두 매칭)
        String normalizedDong = normalizeDong(dong);
        List<Store> stores = storeRepository.findRecommendedByFilters(normalizedDong, category);

        // 2. 각 가게별 방문수(결제 횟수) 집계
        Map<Long, Long> visitMap = payTransactionRepository.countVisitsByStore()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 3. 최대 방문수 (정규화용, 0 방지)
        long maxVisits = visitMap.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1L);

        // 4. 점수 계산 + DTO 변환
        List<RecommendResponse> result = stores.stream()
                .map(store -> {
                    long visitCount = visitMap.getOrDefault(store.getStoreId(), 0L);
                    double normalizedRating = store.getRating() / 5.0;
                    double normalizedVisits = (double) visitCount / maxVisits;
                    double score = (normalizedRating * RATING_WEIGHT) + (normalizedVisits * VISIT_WEIGHT);
                    return RecommendResponse.from(store, visitCount, Math.round(score * 100.0) / 100.0);
                })
                .collect(Collectors.toList());

        // 5. 정렬
        if ("rating".equals(sort)) {
            result.sort(Comparator.comparing(RecommendResponse::getRating).reversed());
        } else if ("visits".equals(sort)) {
            result.sort(Comparator.comparing(RecommendResponse::getVisitCount).reversed());
        } else {
            result.sort(Comparator.comparing(RecommendResponse::getScore).reversed());
        }

        return result;
    }

    /**
     * 좌표 기반 추천 가게 목록 조회.
     */
    public List<RecommendResponse> getRecommendStoresNearby(
            Double lat, Double lng, Double radiusKm, String category, String sort
    ) {
        List<Store> allRecommended = storeRepository.findRecommendedByFilters(null, category);

        List<Store> nearby = allRecommended.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .filter(s -> distanceKm(lat, lng, s.getLatitude(), s.getLongitude()) <= radiusKm)
                .toList();

        Map<Long, Long> visitMap = payTransactionRepository.countVisitsByStore()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        long maxVisits = visitMap.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1L);

        List<RecommendResponse> result = nearby.stream()
                .map(store -> {
                    long visitCount = visitMap.getOrDefault(store.getStoreId(), 0L);
                    double normalizedRating = store.getRating() / 5.0;
                    double normalizedVisits = (double) visitCount / maxVisits;
                    double score = (normalizedRating * RATING_WEIGHT) + (normalizedVisits * VISIT_WEIGHT);
                    return RecommendResponse.from(store, visitCount, Math.round(score * 100.0) / 100.0);
                })
                .collect(Collectors.toList());

        if ("rating".equals(sort)) {
            result.sort(Comparator.comparing(RecommendResponse::getRating).reversed());
        } else if ("visits".equals(sort)) {
            result.sort(Comparator.comparing(RecommendResponse::getVisitCount).reversed());
        } else {
            result.sort(Comparator.comparing(RecommendResponse::getScore).reversed());
        }

        return result;
    }

    private static String normalizeDong(String dong) {
        if (dong == null || dong.isBlank()) return null;
        return dong.replaceAll("(면|읍|동)$", "");
    }

    private static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * 가게가 존재하는 동 목록 조회.
     *
     * @return 동 이름 리스트 (중복 제거, 정렬)
     */
    public List<String> getDongs() {
        return storeRepository.findDistinctDongs();
    }
}
