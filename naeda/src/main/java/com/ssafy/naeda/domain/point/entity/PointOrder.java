package com.ssafy.naeda.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_order")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class PointOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @CreationTimestamp
    @Column(name = "order_at", nullable = false, updatable = false)
    private LocalDateTime orderAt;

    @Column(name = "road_address")
    private String roadAddress;

    @Column(name = "number_address")
    private String numberAddress;
}
