package com.ssafy.naeda.domain.fds.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "fds_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class FdsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fds_id")
    private Long fdsId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "anomaly_score", nullable = false)
    private int anomalyScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "triggered_rules")
    private List<String> triggeredRules;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", nullable = false)
    private FdsAction actionTaken;

    @Column(name = "user_confirmed", nullable = false)
    @Builder.Default
    private Boolean userConfirmed = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime detected;
}
