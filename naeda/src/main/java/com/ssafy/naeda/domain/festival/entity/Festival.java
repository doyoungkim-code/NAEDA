package com.ssafy.naeda.domain.festival.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "festival")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Festival {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "festival_id")
    private Long festivalId;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String location;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "number_address", length = 255)
    private String numberAddress;

    private Double latitude;

    private Double longitude;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "fcm_notified", nullable = false)
    @Builder.Default
    private Boolean fcmNotified = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    public void markFcmNotified() {
        this.fcmNotified = true;
    }

    public void update(String title, String description, String location,
                       String roadAddress, String numberAddress,
                       Double latitude, Double longitude,
                       String linkUrl, String imageUrl,
                       LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.roadAddress = roadAddress;
        this.numberAddress = numberAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.linkUrl = linkUrl;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
