package com.ssafy.naeda.domain.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.store.dto.request.StoreCreateRequest;
import com.ssafy.naeda.domain.store.dto.response.StoreResponse;
import com.ssafy.naeda.domain.store.service.StoreService;
import com.ssafy.naeda.global.exception.GlobalExceptionHandler;
import com.ssafy.naeda.global.exception.NotFoundException;
import com.ssafy.naeda.global.security.JwtAuthenticationFilter;
import com.ssafy.naeda.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── GET /api/stores ──────────────────────────────────────────────────

    @Test
    @DisplayName("매장 목록 조회 - 필터 없이 전체 조회 시 200을 반환한다")
    void getStores_noFilter_returns200() throws Exception {
        given(storeService.getStores(null, null)).willReturn(List.of(
                StoreResponse.builder()
                        .storeId(1L).userNo(10L)
                        .storeName("구미 한식당")
                        .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                        .roadAddress("경북 구미시 대학로 1")
                        .isLocalBusiness(true).facePayEnabled(true).rating(4.5)
                        .build(),
                StoreResponse.builder()
                        .storeId(2L)
                        .storeName("스타벅스").categoryId("CG-9ca85f66311a23d").categoryName("생활")
                        .build()
        ));

        mockMvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].storeName").value("구미 한식당"))
                .andExpect(jsonPath("$[1].storeName").value("스타벅스"));
    }

    @Test
    @DisplayName("매장 목록 조회 - category 필터 적용 시 200을 반환한다")
    void getStores_categoryFilter_returns200() throws Exception {
        given(storeService.getStores("CG-9ca85f66311a23d", null)).willReturn(List.of(
                StoreResponse.builder()
                        .storeId(1L).storeName("구미 한식당")
                        .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                        .roadAddress("경북 구미시 대학로 1")
                        .isLocalBusiness(true).facePayEnabled(true).rating(4.5)
                        .build()
        ));

        mockMvc.perform(get("/api/stores").param("category", "CG-9ca85f66311a23d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoryId").value("CG-9ca85f66311a23d"))
                .andExpect(jsonPath("$[0].categoryName").value("생활"));
    }

    @Test
    @DisplayName("매장 목록 조회 - facePayOnly=true 필터 적용 시 200을 반환한다")
    void getStores_facePayOnlyFilter_returns200() throws Exception {
        given(storeService.getStores(null, true)).willReturn(List.of(
                StoreResponse.builder()
                        .storeId(1L).storeName("구미 한식당")
                        .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                        .roadAddress("경북 구미시 대학로 1")
                        .isLocalBusiness(true).facePayEnabled(true).rating(4.5)
                        .build()
        ));

        mockMvc.perform(get("/api/stores").param("facePayOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].facePayEnabled").value(true));
    }

    // ── GET /api/stores/{storeId} ────────────────────────────────────────

    @Test
    @DisplayName("매장 단건 조회 - 200 OK와 매장 정보를 반환한다")
    void getStore_returns200() throws Exception {
        given(storeService.getStore(1L)).willReturn(
                StoreResponse.builder()
                        .storeId(1L).userNo(10L)
                        .storeName("구미 한식당")
                        .categoryId("CG-9ca85f66311a23d").categoryName("생활")
                        .roadAddress("경북 구미시 대학로 1")
                        .latitude(36.1192).longitude(128.3444)
                        .phone("054-000-0000")
                        .isLocalBusiness(true).facePayEnabled(true).rating(4.5)
                        .build()
        );

        mockMvc.perform(get("/api/stores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(1))
                .andExpect(jsonPath("$.storeName").value("구미 한식당"))
                .andExpect(jsonPath("$.facePayEnabled").value(true))
                .andExpect(jsonPath("$.rating").value(4.5));
    }

    @Test
    @DisplayName("매장 단건 조회 - 존재하지 않는 storeId면 404를 반환한다")
    void getStore_notFound_returns404() throws Exception {
        given(storeService.getStore(999L))
                .willThrow(new NotFoundException("존재하지 않는 매장입니다."));

        mockMvc.perform(get("/api/stores/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 매장입니다."));
    }

    // ── POST /api/stores ─────────────────────────────────────────────────

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("매장 등록 - 201 Created와 등록된 매장 정보를 반환한다")
    void createStore_returns201() throws Exception {
        StoreCreateRequest request = new StoreCreateRequest();
        setField(request, "categoryId", "CG-4fa85f6425ad1d3");
        setField(request, "storeName", "코스트코");
        setField(request, "userNo", 10L);
        setField(request, "accountId", 1L);

        given(storeService.createStore(any(StoreCreateRequest.class))).willReturn(
                StoreResponse.builder()
                        .storeId(3L).userNo(10L)
                        .storeName("코스트코")
                        .categoryId("CG-4fa85f6425ad1d3").categoryName("대형마트")
                        .roadAddress("경북 구미시 산호대로 1")
                        .isLocalBusiness(false).facePayEnabled(true).rating(0.0)
                        .build()
        );

        mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeId").value(3))
                .andExpect(jsonPath("$.storeName").value("코스트코"));
    }
}
