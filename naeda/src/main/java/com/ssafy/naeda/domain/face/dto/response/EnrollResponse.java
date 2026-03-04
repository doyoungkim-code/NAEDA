package com.ssafy.naeda.domain.face.dto.response;

import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollResponse {

    private boolean success;
    private String userId;
    private String pose;
    private LocalDateTime savedAt;

    public static EnrollResponse from(FaceEmbedding entity) {
        return EnrollResponse.builder()
                .success(true)
                .userId(entity.getUserId())
                .pose(entity.getPose())
                .savedAt(entity.getUpdatedAt())
                .build();
    }
}
