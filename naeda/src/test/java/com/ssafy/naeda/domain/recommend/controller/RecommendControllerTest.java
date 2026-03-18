package com.ssafy.naeda.domain.recommend.controller;

import com.ssafy.naeda.domain.recommend.dto.response.RecommendResponse;
import com.ssafy.naeda.domain.recommend.service.RecommendService;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RecommendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendService recommendService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── GET /api/recommend/stores ────────────────────────────────────────

    @Test
    @DisplayName("추천 목록 조회 - 필터 없이 전체 조회 200")
    void getRecommendStores_noFilter() throws Exception {
        given(recommendService.getRecommendStores(null, null, null)).willReturn(List.of(
                RecommendResponse.builder()
                        .storeId(1L).storeName("맛집A").categoryName("한식")
                        .roadAddress("경북 구미시 진평동 1")
                        .rating(4.5).visitCount(30L).score(0.78)
                        .build(),
                RecommendResponse.builder()
                        .storeId(2L).storeName("맛집B").categoryName("양식")
                        .roadAddress("경북 구미시 인의동 2")
                        .rating(3.8).visitCount(50L).score(0.86)
                        .build()
        ));

        mockMvc.perform(get("/api/recommend/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].storeName").value("맛집A"))
                .andExpect(jsonPath("$[1].storeName").value("맛집B"));
    }

    @Test
    @DisplayName("추천 목록 조회 - 동 필터 적용 200")
    void getRecommendStores_dongFilter() throws Exception {
        given(recommendService.getRecommendStores("진평동", null, null)).willReturn(List.of(
                RecommendResponse.builder()
                        .storeId(1L).storeName("맛집A").categoryName("한식")
                        .roadAddress("경북 구미시 진평동 1")
                        .rating(4.5).visitCount(30L).score(0.78)
                        .build()
        ));

        mockMvc.perform(get("/api/recommend/stores").param("dong", "진평동"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roadAddress").value("경북 구미시 진평동 1"));
    }

    @Test
    @DisplayName("추천 목록 조회 - 카테고리 필터 적용 200")
    void getRecommendStores_categoryFilter() throws Exception {
        given(recommendService.getRecommendStores(null, "한식", null)).willReturn(List.of(
                RecommendResponse.builder()
                        .storeId(1L).storeName("맛집A").categoryName("한식")
                        .rating(4.5).visitCount(10L).score(0.58)
                        .build()
        ));

        mockMvc.perform(get("/api/recommend/stores").param("category", "한식"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("한식"));
    }

    @Test
    @DisplayName("추천 목록 조회 - sort=rating 정렬 200")
    void getRecommendStores_sortByRating() throws Exception {
        given(recommendService.getRecommendStores(null, null, "rating")).willReturn(List.of(
                RecommendResponse.builder()
                        .storeId(2L).storeName("맛집B").rating(4.8).visitCount(5L).score(0.6)
                        .build(),
                RecommendResponse.builder()
                        .storeId(1L).storeName("맛집A").rating(3.5).visitCount(50L).score(0.82)
                        .build()
        ));

        mockMvc.perform(get("/api/recommend/stores").param("sort", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(4.8))
                .andExpect(jsonPath("$[1].rating").value(3.5));
    }

    @Test
    @DisplayName("추천 목록 조회 - 동 + 카테고리 + 정렬 조합 200")
    void getRecommendStores_combined() throws Exception {
        given(recommendService.getRecommendStores("진평동", "한식", "visits")).willReturn(List.of(
                RecommendResponse.builder()
                        .storeId(1L).storeName("맛집A").categoryName("한식")
                        .roadAddress("경북 구미시 진평동 1")
                        .rating(4.0).visitCount(100L).score(0.88)
                        .build()
        ));

        mockMvc.perform(get("/api/recommend/stores")
                        .param("dong", "진평동")
                        .param("category", "한식")
                        .param("sort", "visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].visitCount").value(100));
    }

    @Test
    @DisplayName("추천 목록 조회 - 결과 없으면 빈 배열 200")
    void getRecommendStores_empty() throws Exception {
        given(recommendService.getRecommendStores("없는동", null, null)).willReturn(List.of());

        mockMvc.perform(get("/api/recommend/stores").param("dong", "없는동"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/recommend/dongs ─────────────────────────────────────────

    @Test
    @DisplayName("동 목록 조회 - 200")
    void getDongs() throws Exception {
        given(recommendService.getDongs()).willReturn(List.of(
                "경북 구미시 인의동", "경북 구미시 진평동", "경북 구미시 형곡동"
        ));

        mockMvc.perform(get("/api/recommend/dongs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("경북 구미시 인의동"));
    }

    @Test
    @DisplayName("동 목록 조회 - 가게 없으면 빈 배열 200")
    void getDongs_empty() throws Exception {
        given(recommendService.getDongs()).willReturn(List.of());

        mockMvc.perform(get("/api/recommend/dongs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
