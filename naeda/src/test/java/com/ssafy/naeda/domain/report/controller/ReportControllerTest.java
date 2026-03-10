package com.ssafy.naeda.domain.report.controller;

import com.ssafy.naeda.domain.report.dto.request.ReportSaveRequest;
import com.ssafy.naeda.domain.report.entity.LocalGrade;
import com.ssafy.naeda.domain.report.entity.PeriodType;
import com.ssafy.naeda.domain.report.repository.ConsumptionReportRepository;
import com.ssafy.naeda.domain.report.service.ReportService;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ConsumptionReportRepository consumptionReportRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userNo;

    @BeforeEach
    void setUp() {
        consumptionReportRepository.deleteAll();
        User testUser = userRepository.save(User.builder()
                .userId("report-test@test.com")
                .password("pw")
                .username("테스터")
                .residentNo("9901011")
                .phone("010-0000-0000")
                .institutionCode("M220516185630")
                .userKey("test-key")
                .build());
        userNo = testUser.getUserNo();
    }

    @Test
    @DisplayName("GET /api/reports/latest - 최신 리포트 조회 200")
    void getLatestReport() throws Exception {
        reportService.saveReport(ReportSaveRequest.builder()
                .userNo(userNo)
                .periodType(PeriodType.MONTHLY)
                .periodStart(LocalDate.of(2026, 2, 1))
                .periodEnd(LocalDate.of(2026, 2, 28))
                .categoryBreakdown(Map.of("식비", 300000L, "카페", 80000L))
                .totalSpending(380000L)
                .localSpending(200000L)
                .localRatio(0.65f)
                .localGrade(LocalGrade.B)
                .insights(List.of("카페 지출이 전월 대비 15% 증가했습니다"))
                .build());

        mockMvc.perform(get("/api/reports/latest")
                        .param("userNo", userNo.toString())
                        .param("periodType", "MONTHLY"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodType").value("MONTHLY"))
                .andExpect(jsonPath("$.totalSpending").value(380000))
                .andExpect(jsonPath("$.localGrade").value("B"));
    }

    @Test
    @DisplayName("GET /api/reports - 리포트 히스토리 조회 200")
    void getReportHistory() throws Exception {
        reportService.saveReport(ReportSaveRequest.builder()
                .userNo(userNo)
                .periodType(PeriodType.MONTHLY)
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .totalSpending(400000L)
                .build());
        reportService.saveReport(ReportSaveRequest.builder()
                .userNo(userNo)
                .periodType(PeriodType.MONTHLY)
                .periodStart(LocalDate.of(2026, 2, 1))
                .periodEnd(LocalDate.of(2026, 2, 28))
                .totalSpending(500000L)
                .build());

        mockMvc.perform(get("/api/reports")
                        .param("userNo", userNo.toString())
                        .param("periodType", "MONTHLY"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/reports/latest - 필수 파라미터 누락 시 400")
    void getLatestReportMissingParam() throws Exception {
        mockMvc.perform(get("/api/reports/latest")
                        .param("userNo", "1"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
