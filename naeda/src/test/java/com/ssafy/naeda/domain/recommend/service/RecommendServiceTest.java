package com.ssafy.naeda.domain.recommend.service;

import com.ssafy.naeda.domain.pay.repository.PayTransactionRepository;
import com.ssafy.naeda.domain.recommend.dto.response.RecommendResponse;
import com.ssafy.naeda.domain.store.entity.Store;
import com.ssafy.naeda.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @InjectMocks
    private RecommendService recommendService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PayTransactionRepository payTransactionRepository;

    private Store store(Long id, String name, String address, String category, double rating) {
        return Store.builder()
                .storeId(id)
                .storeName(name)
                .roadAddress(address)
                .categoryName(category)
                .rating(rating)
                .isRecommended(true)
                .isActive(true)
                .build();
    }

    // ── getRecommendStores ───────────────────────────────────────────────

    @Test
    @DisplayName("전체 조회 - 기본 추천 점수순 정렬")
    void getRecommendStores_defaultSort() {
        Store s1 = store(1L, "맛집A", "경북 구미시 진평동 1", "한식", 4.5);
        Store s2 = store(2L, "맛집B", "경북 구미시 인의동 2", "양식", 3.0);

        given(storeRepository.findRecommendedByFilters(null, null)).willReturn(List.of(s1, s2));
        given(payTransactionRepository.countVisitsByStore()).willReturn(List.of(
                new Object[]{1L, 10L},
                new Object[]{2L, 50L}
        ));

        List<RecommendResponse> result = recommendService.getRecommendStores(null, null, null);

        assertThat(result).hasSize(2);
        // s2: rating 3.0 but visits 50 (max) → score higher due to visit weight
        // 확인: 점수 내림차순
        assertThat(result.get(0).getScore()).isGreaterThanOrEqualTo(result.get(1).getScore());
    }

    @Test
    @DisplayName("별점순 정렬 - sort=rating")
    void getRecommendStores_sortByRating() {
        Store s1 = store(1L, "맛집A", "경북 구미시 진평동 1", "한식", 3.0);
        Store s2 = store(2L, "맛집B", "경북 구미시 인의동 2", "양식", 4.8);

        given(storeRepository.findRecommendedByFilters(null, null)).willReturn(List.of(s1, s2));
        given(payTransactionRepository.countVisitsByStore()).willReturn(List.of());

        List<RecommendResponse> result = recommendService.getRecommendStores(null, null, "rating");

        assertThat(result.get(0).getRating()).isEqualTo(4.8);
        assertThat(result.get(1).getRating()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("방문순 정렬 - sort=visits")
    void getRecommendStores_sortByVisits() {
        Store s1 = store(1L, "맛집A", "경북 구미시 진평동 1", "한식", 4.5);
        Store s2 = store(2L, "맛집B", "경북 구미시 인의동 2", "양식", 3.0);

        given(storeRepository.findRecommendedByFilters(null, null)).willReturn(List.of(s1, s2));
        given(payTransactionRepository.countVisitsByStore()).willReturn(List.of(
                new Object[]{1L, 5L},
                new Object[]{2L, 30L}
        ));

        List<RecommendResponse> result = recommendService.getRecommendStores(null, null, "visits");

        assertThat(result.get(0).getVisitCount()).isEqualTo(30L);
        assertThat(result.get(1).getVisitCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("동 필터 적용")
    void getRecommendStores_dongFilter() {
        Store s1 = store(1L, "맛집A", "경북 구미시 진평동 1", "한식", 4.0);

        given(storeRepository.findRecommendedByFilters("진평동", null)).willReturn(List.of(s1));
        given(payTransactionRepository.countVisitsByStore()).willReturn(List.of());

        List<RecommendResponse> result = recommendService.getRecommendStores("진평동", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStoreName()).isEqualTo("맛집A");
    }

    @Test
    @DisplayName("카테고리 필터 적용")
    void getRecommendStores_categoryFilter() {
        Store s1 = store(1L, "맛집A", "경북 구미시 진평동 1", "한식", 4.0);

        given(storeRepository.findRecommendedByFilters(null, "한식")).willReturn(List.of(s1));
        given(payTransactionRepository.countVisitsByStore()).willReturn(List.of());

        List<RecommendResponse> result = recommendService.getRecommendStores(null, "한식", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("한식");
    }

    @Test
    @DisplayName("동 + 카테고리 + 정렬 조합")
    void getRecommendStores_combinedFilters() {
        Store s1 = store(1L, "맛집A", "경북 구미시 진평동 1", "한식", 4.5);
        Store s2 = store(2L, "맛집B", "경북 구미시 진평동 2", "한식", 3.8);

        given(storeRepository.findRecommendedByFilters("진평동", "한식")).willReturn(List.of(s1, s2));
        given(payTransactionRepository.countVisitsByStore()).willReturn(List.of(
                new Object[]{1L, 10L},
                new Object[]{2L, 20L}
        ));

        List<RecommendResponse> result = recommendService.getRecommendStores("진평동", "한식", "rating");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("방문 기록 없는 가게 - visitCount 0, score는 rating 기반만")
    void getRecommendStores_noVisits() {
        Store s1 = store(1L, "신규맛집", "경북 구미시 진평동 1", "한식", 4.0);

        given(storeRepository.findRecommendedByFilters(null, null)).willReturn(List.of(s1));
        given(payTransactionRepository.countVisitsByStore()).willReturn(List.of());

        List<RecommendResponse> result = recommendService.getRecommendStores(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVisitCount()).isEqualTo(0L);
        // score = (4.0/5.0) * 0.6 + (0/1) * 0.4 = 0.48
        assertThat(result.get(0).getScore()).isEqualTo(0.48);
    }

    // ── getDongs ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("동 목록 조회")
    void getDongs() {
        given(storeRepository.findDistinctDongs()).willReturn(List.of("경북 구미시 인의동", "경북 구미시 진평동"));

        List<String> result = recommendService.getDongs();

        assertThat(result).hasSize(2);
        assertThat(result).contains("경북 구미시 인의동", "경북 구미시 진평동");
    }
}
