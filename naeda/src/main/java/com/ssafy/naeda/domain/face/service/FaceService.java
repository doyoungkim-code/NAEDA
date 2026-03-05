package com.ssafy.naeda.domain.face.service;

import com.ssafy.naeda.domain.face.client.AiClient;
import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResult;
import com.ssafy.naeda.domain.face.dto.response.CandidateDto;
import com.ssafy.naeda.domain.face.dto.response.EnrollResponse;
import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.face.dto.response.HeadPoseCheckResponse;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import com.ssafy.naeda.domain.face.exception.FaceErrorCode;
import com.ssafy.naeda.domain.face.exception.FaceException;
import com.ssafy.naeda.domain.face.repository.FaceEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaceService {

    private final AiClient aiClient;
    private final FaceEmbeddingRepository faceEmbeddingRepository;

    @Value("${face.threshold.match:0.7}")
    private float matchThreshold;

    @Value("${face.threshold.ambiguous:0.65}")
    private float ambiguousThreshold;

    private static final Set<String> VALID_POSES = Set.of("front1", "front2", "front3", "left", "right", "up", "down");
    private static final Set<String> VALID_HEADPOSE_DIRECTIONS = Set.of("front", "left", "right", "up", "down");

    /**
     * 얼굴 등록
     * 1. pose 유효성 검사
     * 2. AI 서버에서 임베딩 추출
     * 3. DB에 upsert (같은 userId+pose면 덮어씀)
     */
    @Transactional
    public EnrollResponse enroll(String userId, String pose, MultipartFile image) {
        if (!VALID_POSES.contains(pose)) {
            throw new FaceException(FaceErrorCode.INVALID_POSE);
        }

        AiEmbeddingResult embeddingResult = aiClient.extractEmbedding(image);
        float[] embedding = embeddingResult.getEmbedding();

        // 이미 등록된 (userId, pose) 조합이면 업데이트, 없으면 새로 저장
        FaceEmbedding entity = faceEmbeddingRepository.findByUserIdAndPose(userId, pose)
                .map(existing -> {
                    existing.updateEmbedding(embedding);
                    return existing;
                })
                .orElseGet(() -> FaceEmbedding.builder()
                        .userId(userId)
                        .pose(pose)
                        .embedding(embedding)
                        .build());

        faceEmbeddingRepository.save(entity);

        log.info("얼굴 등록 완료: userId={}, pose={}", userId, pose);
        return EnrollResponse.from(entity);
    }

    /**
     * 얼굴 검색
     * 1. AI 서버에서 임베딩 추출
     * 2. DB의 모든 임베딩과 코사인 유사도 계산
     * 3. 유사도 높은 순으로 topK 반환, threshold(0.7) 이상이면 matched
     */
    public SearchResponse search(MultipartFile image, int topK) {
        AiEmbeddingResult probeResult = aiClient.extractEmbedding(image);
        float[] probe = probeResult.getEmbedding();

        List<FaceEmbedding> all = faceEmbeddingRepository.findAll();

        List<CandidateDto> candidates = all.stream()
                .map(e -> new CandidateDto(e.getUserId(), e.getPose(), cosineSimilarity(probe, e.getEmbedding())))
                .sorted(Comparator.comparingDouble(CandidateDto::getSimilarity).reversed())
                .limit(topK)
                .toList();

        CandidateDto best = candidates.isEmpty() ? null : candidates.get(0);
        float bestSimilarity = best != null ? best.getSimilarity() : 0f;
        FaceMatchStatus status = resolveStatus(bestSimilarity);
        boolean matched = status == FaceMatchStatus.MATCH;
        String nextAction = switch (status) {
            case MATCH -> "PASS";
            case AMBIGUOUS -> "REQUIRE_SECOND_FACTOR";
            case NO_MATCH -> "RETRY_CAPTURE";
        };

        return SearchResponse.builder()
                .matched(matched)
                .status(status)
                .nextAction(nextAction)
                .bestUserId(status == FaceMatchStatus.NO_MATCH || best == null ? null : best.getUserId())
                .similarity(bestSimilarity)
                .matchThreshold(matchThreshold)
                .ambiguousThreshold(ambiguousThreshold)
                .qualityScore(probeResult.getQualityScore())
                .yaw(probeResult.getYaw())
                .pitch(probeResult.getPitch())
                .roll(probeResult.getRoll())
                .candidates(candidates)
                .build();
    }

    private FaceMatchStatus resolveStatus(float similarity) {
        if (similarity >= matchThreshold) {
            return FaceMatchStatus.MATCH;
        }
        if (similarity >= ambiguousThreshold) {
            return FaceMatchStatus.AMBIGUOUS;
        }
        return FaceMatchStatus.NO_MATCH;
    }

    /**
     * 얼굴 방향 검증
     * 1. 기대 방향 유효성 검사
     * 2. AI headpose API 호출
     * 3. 방향 일치 여부 및 점수 반환
     */
    public HeadPoseCheckResponse checkHeadPoseDirection(String expectedDirection, MultipartFile image) {
        String normalized = expectedDirection == null ? "" : expectedDirection.toLowerCase();
        if (!VALID_HEADPOSE_DIRECTIONS.contains(normalized)) {
            throw new FaceException(FaceErrorCode.INVALID_POSE);
        }

        return HeadPoseCheckResponse.from(aiClient.checkHeadPose(normalized, image));
    }

    /**
     * 코사인 유사도 계산
     * 두 벡터가 얼마나 비슷한지를 -1 ~ 1 사이 값으로 반환 (1에 가까울수록 동일인)
     */
    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0f, normA = 0f, normB = 0f;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot  += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0f || normB == 0f) return 0f;
        return dot / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
