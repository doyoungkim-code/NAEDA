package com.ssafy.naeda.domain.notice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "fcm_notified", nullable = false)
    @Builder.Default
    private Boolean fcmNotified = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    private LocalDateTime modified;

    @PreUpdate
    protected void onUpdate() {
        this.modified = LocalDateTime.now();
    }

    public void markFcmNotified() {
        this.fcmNotified = true;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
