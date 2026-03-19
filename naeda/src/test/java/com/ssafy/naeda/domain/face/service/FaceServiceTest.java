package com.ssafy.naeda.domain.face.service;

import com.ssafy.naeda.domain.face.client.AiClient;
import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResult;
import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import com.ssafy.naeda.domain.face.exception.FaceException;
import com.ssafy.naeda.domain.face.repository.FaceEmbeddingRepository;
import com.ssafy.naeda.domain.rba.dto.AuthLevel;
import com.ssafy.naeda.domain.rba.dto.AuthMethod;
import com.ssafy.naeda.domain.rba.dto.RbaResult;
import com.ssafy.naeda.domain.rba.service.RbaEngine;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FaceServiceTest {

    @InjectMocks
    private FaceService faceService;

    @Mock
    private AiClient aiClient;

    @Mock
    private FaceEmbeddingRepository faceEmbeddingRepository;

    @Mock
    private RbaEngine rbaEngine;

    @Mock
    private FaceInputValidator faceInputValidator;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(faceService, "matchThreshold", 0.7f);
        ReflectionTestUtils.setField(faceService, "ambiguousThreshold", 0.65f);
    }

    @Test
    @DisplayName("search: similarity가 0.70 이상이면 MATCH와 PASS를 반환한다")
    void search_match() {
        given(aiClient.extractEmbedding(any())).willReturn(aiResult(unit(1f, 0f)));
        given(userRepository.findByUserId("user-match")).willReturn(Optional.of(User.builder().userNo(11L).username("매치유저").build()));
        given(userRepository.findByUserId("user-other")).willReturn(Optional.of(User.builder().userNo(22L).username("다른유저").build()));
        given(rbaEngine.evaluate(anyLong(), any(), anyDouble())).willReturn(
                RbaResult.builder()
                        .authLevel(AuthLevel.FACE_ONLY)
                        .requiredMethods(Set.of(AuthMethod.FACE))
                        .blocked(false)
                        .reason("ok")
                        .build()
        );
        given(faceEmbeddingRepository.findAll()).willReturn(List.of(
                embedding("user-match", "front1", unit(1f, 0f)),
                embedding("user-other", "left", unit(0f, 1f))
        ));

        SearchResponse response = faceService.search(mockImage(), 3, 30_000L);

        assertThat(response.isMatched()).isTrue();
        assertThat(response.getStatus()).isEqualTo(FaceMatchStatus.MATCH);
        assertThat(response.getNextAction()).isEqualTo("PASS");
        assertThat(response.getAuthLevel()).isEqualTo(AuthLevel.FACE_ONLY);
        assertThat(response.isBlocked()).isFalse();
        assertThat(response.getRequiredMethods()).containsExactly(AuthMethod.FACE);
        assertThat(response.getBestUserId()).isEqualTo("user-match");
        assertThat(response.getUsername()).isEqualTo("매치유저");
        assertThat(response.getUserNo()).isEqualTo(11L);
        assertThat(response.getMatchedUserNo()).isEqualTo(11L);
        assertThat(response.getSimilarity()).isGreaterThanOrEqualTo(0.7f);
        assertThat(response.getAiProcessing()).isNotNull();
        assertThat(response.getAiProcessing().getAiStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAiProcessing().isFallbackUsed()).isFalse();
    }

    @Test
    @DisplayName("search: similarity가 0.65 이상 0.70 미만이면 AMBIGUOUS를 반환한다")
    void search_ambiguous() {
        given(aiClient.extractEmbedding(any())).willReturn(aiResult(unit(1f, 0f)));
        given(userRepository.findByUserId("user-ambiguous")).willReturn(Optional.of(User.builder().userNo(33L).username("애매유저").build()));
        given(userRepository.findByUserId("user-low")).willReturn(Optional.of(User.builder().userNo(44L).username("낮은유저").build()));
        given(rbaEngine.evaluate(anyLong(), any(), anyDouble())).willReturn(
                RbaResult.builder()
                        .authLevel(AuthLevel.FACE_PHONE)
                        .requiredMethods(Set.of(AuthMethod.FACE, AuthMethod.PHONE))
                        .blocked(false)
                        .reason("need second")
                        .build()
        );
        given(faceEmbeddingRepository.findAll()).willReturn(List.of(
                embedding("user-ambiguous", "front1", unit(0.68f, (float) Math.sqrt(1 - 0.68f * 0.68f))),
                embedding("user-low", "left", unit(0.1f, (float) Math.sqrt(1 - 0.1f * 0.1f)))
        ));

        SearchResponse response = faceService.search(mockImage(), 3, 30_000L);

        assertThat(response.isMatched()).isFalse();
        assertThat(response.getStatus()).isEqualTo(FaceMatchStatus.AMBIGUOUS);
        assertThat(response.getNextAction()).isEqualTo("REQUIRE_SECOND_FACTOR");
        assertThat(response.getAuthLevel()).isEqualTo(AuthLevel.FACE_PHONE);
        assertThat(response.isBlocked()).isFalse();
        assertThat(response.getRequiredMethods()).contains(AuthMethod.FACE, AuthMethod.PHONE);
        assertThat(response.getBestUserId()).isEqualTo("user-ambiguous");
        assertThat(response.getUsername()).isEqualTo("애매유저");
        assertThat(response.getUserNo()).isEqualTo(33L);
        assertThat(response.getMatchedUserNo()).isEqualTo(33L);
        assertThat(response.getSimilarity()).isBetween(0.65f, 0.7f);
    }

    @Test
    @DisplayName("search: similarity가 0.65 미만이면 NO_MATCH와 RETRY_CAPTURE를 반환한다")
    void search_noMatch() {
        given(aiClient.extractEmbedding(any())).willReturn(aiResult(unit(1f, 0f)));
        given(userRepository.findByUserId("user-low")).willReturn(Optional.of(User.builder().userNo(44L).username("낮은유저").build()));
        given(rbaEngine.evaluate(anyLong(), any(), anyDouble())).willReturn(
                RbaResult.builder()
                        .authLevel(AuthLevel.BLOCKED)
                        .requiredMethods(Set.of())
                        .blocked(true)
                        .reason("blocked")
                        .build()
        );
        given(faceEmbeddingRepository.findAll()).willReturn(List.of(
                embedding("user-low", "front1", unit(0.4f, (float) Math.sqrt(1 - 0.4f * 0.4f)))
        ));

        SearchResponse response = faceService.search(mockImage(), 3, 30_000L);

        assertThat(response.isMatched()).isFalse();
        assertThat(response.getStatus()).isEqualTo(FaceMatchStatus.NO_MATCH);
        assertThat(response.getNextAction()).isEqualTo("BLOCK");
        assertThat(response.getAuthLevel()).isEqualTo(AuthLevel.BLOCKED);
        assertThat(response.isBlocked()).isTrue();
        assertThat(response.getRequiredMethods()).isEmpty();
        assertThat(response.getBestUserId()).isNull();
        assertThat(response.getUsername()).isNull();
        assertThat(response.getUserNo()).isNull();
        assertThat(response.getMatchedUserNo()).isNull();
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
                .fallbackUsed(false)
                .aiStatus("COMPLETED")
                .message("Primary inference succeeded.")
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
