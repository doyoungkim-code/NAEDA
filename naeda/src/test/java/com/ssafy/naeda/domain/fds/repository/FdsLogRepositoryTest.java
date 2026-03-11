package com.ssafy.naeda.domain.fds.repository;

import com.ssafy.naeda.domain.fds.entity.FdsAction;
import com.ssafy.naeda.domain.fds.entity.FdsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FdsLogRepositoryTest {

    @Autowired
    private FdsLogRepository fdsLogRepository;

    @BeforeEach
    void setUp() {
        fdsLogRepository.deleteAll();
    }

    @Test
    @DisplayName("FdsLog 저장 및 단건 조회")
    void saveAndFindById() {
        FdsLog log = fdsLogRepository.save(FdsLog.builder()
                .paymentId(1L)
                .userNo(100L)
                .anomalyScore(45)
                .triggeredRules(List.of("LATE_NIGHT", "HIGH_AMOUNT"))
                .actionTaken(FdsAction.ALERT)
                .build());

        FdsLog found = fdsLogRepository.findById(log.getFdsId()).orElseThrow();
        assertThat(found.getPaymentId()).isEqualTo(1L);
        assertThat(found.getUserNo()).isEqualTo(100L);
        assertThat(found.getAnomalyScore()).isEqualTo(45);
        assertThat(found.getTriggeredRules()).containsExactly("LATE_NIGHT", "HIGH_AMOUNT");
        assertThat(found.getActionTaken()).isEqualTo(FdsAction.ALERT);
        assertThat(found.getUserConfirmed()).isFalse();
        assertThat(found.getDetected()).isNotNull();
    }

    @Test
    @DisplayName("paymentId로 FdsLog 조회")
    void findByPaymentId() {
        fdsLogRepository.save(FdsLog.builder()
                .paymentId(10L)
                .userNo(100L)
                .anomalyScore(70)
                .triggeredRules(List.of("FREQUENCY"))
                .actionTaken(FdsAction.PAUSE)
                .build());

        Optional<FdsLog> found = fdsLogRepository.findByPaymentId(10L);
        assertThat(found).isPresent();
        assertThat(found.get().getAnomalyScore()).isEqualTo(70);

        Optional<FdsLog> notFound = fdsLogRepository.findByPaymentId(999L);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("userNo로 FdsLog 목록 조회")
    void findByUserNo() {
        fdsLogRepository.save(FdsLog.builder()
                .paymentId(1L).userNo(100L).anomalyScore(20)
                .triggeredRules(List.of()).actionTaken(FdsAction.NONE).build());
        fdsLogRepository.save(FdsLog.builder()
                .paymentId(2L).userNo(100L).anomalyScore(85)
                .triggeredRules(List.of("LATE_NIGHT", "HIGH_AMOUNT")).actionTaken(FdsAction.BLOCK).build());
        fdsLogRepository.save(FdsLog.builder()
                .paymentId(3L).userNo(200L).anomalyScore(30)
                .triggeredRules(List.of("FREQUENCY")).actionTaken(FdsAction.ALERT).build());

        List<FdsLog> user100Logs = fdsLogRepository.findByUserNo(100L);
        assertThat(user100Logs).hasSize(2);
        assertThat(user100Logs).extracting(FdsLog::getUserNo).containsOnly(100L);

        List<FdsLog> user200Logs = fdsLogRepository.findByUserNo(200L);
        assertThat(user200Logs).hasSize(1);

        List<FdsLog> emptyLogs = fdsLogRepository.findByUserNo(999L);
        assertThat(emptyLogs).isEmpty();
    }
}
