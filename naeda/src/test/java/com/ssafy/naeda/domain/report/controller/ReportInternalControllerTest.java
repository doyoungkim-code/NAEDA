package com.ssafy.naeda.domain.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.naeda.domain.report.dto.request.ReportSaveRequest;
import com.ssafy.naeda.domain.report.entity.LocalGrade;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import com.ssafy.naeda.domain.report.repository.ConsumptionReportRepository;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ReportInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsumptionReportRepository consumptionReportRepository;

    @Autowired
    private UserRepository userRepository;

    private Long testUserNo;

    @BeforeEach
    void setUp() {
        consumptionReportRepository.deleteAll();
        User testUser = userRepository.save(User.builder()
                .userId("internal-test@test.com")
                .password("pw")
                .username("테스터")
                .residentNo("9901011")
                .phone("010-1111-1111")
                .institutionCode("M220516185630")
                .userKey("test-key")
                .build());
        testUserNo = testUser.getUserNo();
    }

    @Test
    @DisplayName("POST /api/internal/reports - 리포트 저장 201")
    void saveReport() throws Exception {
        ReportSaveRequest request = ReportSaveRequest.builder()
                .userNo(testUserNo)
                .periodType(PeriodType.MONTHLY)
                .periodStart(LocalDate.of(2026, 2, 1))
                .periodEnd(LocalDate.of(2026, 2, 28))
                .categoryBreakdown(Map.of("식비", 300000L, "카페", 80000L))
                .totalSpending(380000L)
                .localSpending(200000L)
                .localRatio(0.65f)
                .localGrade(LocalGrade.B)
                .insights(List.of("카페 지출이 전월 대비 15% 증가했습니다"))
                .build();

        mockMvc.perform(post("/api/internal/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").exists())
                .andExpect(jsonPath("$.periodType").value("MONTHLY"))
                .andExpect(jsonPath("$.totalSpending").value(380000));
    }

    @Test
    @DisplayName("POST /api/internal/reports - 필수 값 누락 시 400")
    void saveReportValidationFail() throws Exception {
        String invalidJson = "{\"totalSpending\": 100000}";

        mockMvc.perform(post("/api/internal/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
