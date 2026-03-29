package com.ssafy.naeda.domain.face.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "face_embeddings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "pose"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FaceEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "pose", nullable = false, length = 16)
    private String pose;

    // float[] 을 TEXT(JSON 배열)로 변환해서 저장
    @Convert(converter = FloatArrayConverter.class)
    @Column(name = "embedding", columnDefinition = "TEXT", nullable = false)
    private float[] embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
