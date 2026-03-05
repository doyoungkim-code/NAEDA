package com.ssafy.naeda.domain.face.service;

import com.ssafy.naeda.domain.face.client.AiClient;
import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResult;
import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import com.ssafy.naeda.domain.face.exception.FaceException;
import com.ssafy.naeda.domain.face.repository.FaceEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FaceServiceTest {

    @InjectMocks
    private FaceService faceService;

    @Mock
    private AiClient aiClient;

    @Mock
    private FaceEmbeddingRepository faceEmbeddingRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(faceService, "matchThreshold", 0.7f);
        ReflectionTestUtils.setField(faceService, "ambiguousThreshold", 0.65f);
    }

    @Test
    @DisplayName("search: similarity가 0.70 이상이면 MATCH와 PASS를 반환한다")
    void search_match() {
        given(aiClient.extractEmbedding(any())).willReturn(aiResult(unit(1f, 0f)));
        given(faceEmbeddingRepository.findAll()).willReturn(List.of(
                embedding("user-match", "front1", unit(1f, 0f)),
                embedding("user-other", "left", unit(0f, 1f))
        ));

        SearchResponse response = faceService.search(mockImage(), 3);

        assertThat(response.isMatched()).isTrue();
        assertThat(response.getStatus()).isEqualTo(FaceMatchStatus.MATCH);
        assertThat(response.getNextAction()).isEqualTo("PASS");
        assertThat(response.getBestUserId()).isEqualTo("user-match");
        assertThat(response.getSimilarity()).isGreaterThanOrEqualTo(0.7f);
    }

    @Test
    @DisplayName("search: similarity가 0.65 이상 0.70 미만이면 AMBIGUOUS를 반환한다")
    void search_ambiguous() {
        given(aiClient.extractEmbedding(any())).willReturn(aiResult(unit(1f, 0f)));
        given(faceEmbeddingRepository.findAll()).willReturn(List.of(
                embedding("user-ambiguous", "front1", unit(0.68f, (float) Math.sqrt(1 - 0.68f * 0.68f))),
                embedding("user-low", "left", unit(0.1f, (float) Math.sqrt(1 - 0.1f * 0.1f)))
        ));

        SearchResponse response = faceService.search(mockImage(), 3);

        assertThat(response.isMatched()).isFalse();
        assertThat(response.getStatus()).isEqualTo(FaceMatchStatus.AMBIGUOUS);
        assertThat(response.getNextAction()).isEqualTo("REQUIRE_SECOND_FACTOR");
        assertThat(response.getBestUserId()).isEqualTo("user-ambiguous");
        assertThat(response.getSimilarity()).isBetween(0.65f, 0.7f);
    }

    @Test
    @DisplayName("search: similarity가 0.65 미만이면 NO_MATCH와 RETRY_CAPTURE를 반환한다")
    void search_noMatch() {
        given(aiClient.extractEmbedding(any())).willReturn(aiResult(unit(1f, 0f)));
        given(faceEmbeddingRepository.findAll()).willReturn(List.of(
                embedding("user-low", "front1", unit(0.4f, (float) Math.sqrt(1 - 0.4f * 0.4f)))
        ));

        SearchResponse response = faceService.search(mockImage(), 3);

        assertThat(response.isMatched()).isFalse();
        assertThat(response.getStatus()).isEqualTo(FaceMatchStatus.NO_MATCH);
        assertThat(response.getNextAction()).isEqualTo("RETRY_CAPTURE");
        assertThat(response.getBestUserId()).isNull();
        assertThat(response.getSimilarity()).isLessThan(0.65f);
    }

    @Test
    @DisplayName("enroll: 유효하지 않은 pose이면 INVALID_POSE 예외를 던진다")
    void enroll_invalidPose() {
        assertThatThrownBy(() -> faceService.enroll("user-1", "invalid-pose", mockImage()))
                .isInstanceOf(FaceException.class)
                .hasMessage("올바르지 않은 포즈입니다. (front|left|right|up|down)");
    }

    private static AiEmbeddingResult aiResult(float[] embedding) {
        return AiEmbeddingResult.builder()
                .embedding(embedding)
                .qualityScore(0.9f)
                .model("arcface-buffalo_l")
                .faceCount(1)
                .yaw(0.0f)
                .pitch(0.0f)
                .roll(0.0f)
                .build();
    }

    private static FaceEmbedding embedding(String userId, String pose, float[] emb) {
        return FaceEmbedding.builder()
                .userId(userId)
                .pose(pose)
                .embedding(emb)
                .build();
    }

    private static float[] unit(float x, float y) {
        float norm = (float) Math.sqrt(x * x + y * y);
        return new float[]{x / norm, y / norm};
    }

    private static MockMultipartFile mockImage() {
        return new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }
}
