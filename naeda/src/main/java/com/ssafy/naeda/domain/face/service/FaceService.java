package com.ssafy.naeda.domain.face.service;

import com.ssafy.naeda.domain.face.client.AiClient;
import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResult;
import com.ssafy.naeda.domain.face.dto.response.AiProcessingInfo;
import com.ssafy.naeda.domain.face.dto.response.CandidateDto;
import com.ssafy.naeda.domain.face.dto.response.EnrollResponse;
import com.ssafy.naeda.domain.face.dto.response.FaceMatchStatus;
import com.ssafy.naeda.domain.face.dto.response.HeadPoseCheckResponse;
import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import com.ssafy.naeda.domain.face.exception.FaceErrorCode;
import com.ssafy.naeda.domain.face.exception.FaceException;
import com.ssafy.naeda.domain.face.repository.FaceEmbeddingRepository;
import com.ssafy.naeda.domain.rba.dto.RbaResult;
import com.ssafy.naeda.domain.rba.service.RbaEngine;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaceService {

    private final AiClient aiClient;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final RbaEngine rbaEngine;
    private final FaceInputValidator faceInputValidator;
    private final UserRepository userRepository;
    private final FaceRegistrationSessionService faceRegistrationSessionService;

    @Value("${face.threshold.match:0.7}")
    private float matchThreshold;

    @Value("${face.threshold.ambiguous:0.65}")
    private float ambiguousThreshold;

    private static final Set<String> VALID_HEADPOSE_DIRECTIONS = Set.of("front", "left", "right", "up", "down");

    /**
     * 얼굴 등록
     * 1. pose 유효성 검사
     * 2. AI 서버에서 임베딩 추출
     * 3. 등록 세션에 임시 저장 및 동일인 검증
     */
    @Transactional
    public EnrollResponse enroll(String userId, String pose, MultipartFile image) {
        faceInputValidator.validateImage(image);
        AiEmbeddingResult embeddingResult = aiClient.extractEmbedding(image);
        EnrollResponse response = faceRegistrationSessionService.registerPose(userId, pose, embeddingResult);
        log.info("얼굴 등록 세션 업데이트 완료: userId={}, pose={}", userId, pose);
        return response;
    }

    /**
     * 얼굴 검색
     * 1. AI 서버에서 임베딩 추출
     * 2. DB의 모든 임베딩과 코사인 유사도 계산
     * 3. 유사도 높은 순으로 topK 반환, threshold 이상이면 matched
     */
    public SearchResponse search(MultipartFile image, int topK, long amount) {
        faceInputValidator.validateImage(image);
        AiEmbeddingResult probeResult = aiClient.extractEmbedding(image);
        float[] probe = probeResult.getEmbedding();

        List<FaceEmbedding> all = faceEmbeddingRepository.findAll();

        List<CandidateDto> candidates = all.stream()
                .map(e -> new CandidateDto(
                        e.getUserId(),
                        resolveUserNo(e.getUserId()),
                        e.getPose(),
                        cosineSimilarity(probe, e.getEmbedding())
                ))
                .sorted(Comparator.comparingDouble(CandidateDto::getSimilarity).reversed())
                .limit(topK)
                .toList();

        CandidateDto best = candidates.isEmpty() ? null : candidates.get(0);
        float bestSimilarity = best != null ? best.getSimilarity() : 0f;
        FaceMatchStatus status = resolveStatus(bestSimilarity);
        RbaResult rbaResult = rbaEngine.evaluate(amount, status, bestSimilarity);
        boolean matched = status == FaceMatchStatus.MATCH;
        String nextAction = switch (rbaResult.getAuthLevel()) {
            case FACE_ONLY -> "PASS";
            case BLOCKED -> "BLOCK";
            default -> "REQUIRE_SECOND_FACTOR";
        };
        ResolvedUser bestUser = status == FaceMatchStatus.NO_MATCH || best == null
                ? null
                : resolveUser(best.getUserId());

        return SearchResponse.builder()
                .matched(matched)
                .status(status)
                .nextAction(nextAction)
                .bestUserId(status == FaceMatchStatus.NO_MATCH || best == null ? null : best.getUserId())
                .username(bestUser == null ? null : bestUser.username())
                .userNo(bestUser == null ? null : bestUser.userNo())
                .matchedUserNo(bestUser == null ? null : bestUser.userNo())
                .similarity(bestSimilarity)
                .matchThreshold(matchThreshold)
                .ambiguousThreshold(ambiguousThreshold)
                .qualityScore(probeResult.getQualityScore())
                .yaw(probeResult.getYaw())
                .pitch(probeResult.getPitch())
                .roll(probeResult.getRoll())
                .authLevel(rbaResult.getAuthLevel())
                .requiredMethods(rbaResult.getRequiredMethods())
                .blocked(rbaResult.isBlocked())
                .rbaReason(rbaResult.getReason())
                .candidates(candidates)
                .aiProcessing(AiProcessingInfo.from(probeResult))
                .build();
    }

    private Long resolveUserNo(String userId) {
        return userRepository.findByUserId(userId)
                .map(user -> user.getUserNo())
                .orElse(null);
    }

    private ResolvedUser resolveUser(String userId) {
        return userRepository.findByUserId(userId)
                .map(user -> new ResolvedUser(user.getUserNo(), user.getUsername()))
                .orElse(null);
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

    public HeadPoseCheckResponse checkHeadPoseDirection(String expectedDirection, MultipartFile image) {
        faceInputValidator.validateImage(image);
        String normalized = expectedDirection == null ? "" : expectedDirection.toLowerCase();
        if (!VALID_HEADPOSE_DIRECTIONS.contains(normalized)) {
            throw new FaceException(FaceErrorCode.INVALID_POSE);
        }

        return HeadPoseCheckResponse.from(aiClient.checkHeadPose(normalized, image));
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0f;
        float normA = 0f;
        float normB = 0f;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0f || normB == 0f) {
            return 0f;
        }
        return dot / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record ResolvedUser(Long userNo, String username) {
    }
}
