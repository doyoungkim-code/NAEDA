package com.ssafy.naeda.domain.face.service;

import com.ssafy.naeda.domain.face.client.dto.AiEmbeddingResult;
import com.ssafy.naeda.domain.face.dto.response.AiProcessingInfo;
import com.ssafy.naeda.domain.face.dto.response.EnrollResponse;
import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import com.ssafy.naeda.domain.face.exception.FaceErrorCode;
import com.ssafy.naeda.domain.face.exception.FaceException;
import com.ssafy.naeda.domain.face.repository.FaceEmbeddingRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaceRegistrationSessionService {

    private static final Set<String> VALID_POSES = Set.of("front1", "front2", "front3", "left", "right", "up", "down");
    private static final Set<String> REQUIRED_POSES = Set.of("front1", "front2", "front3", "left", "right", "up", "down");

    private static final long SESSION_TTL_SECONDS = 20 * 60L;

    private static final float FRONT_MATCH_THRESHOLD = 0.75f;
    private static final float SIDE_MATCH_THRESHOLD = 0.62f;
    private static final float VERTICAL_MATCH_THRESHOLD = 0.58f;

    private static final float FRONT1_QUALITY_THRESHOLD = 0.55f;
    private static final float FRONT_QUALITY_THRESHOLD = 0.50f;
    private static final float PROFILE_QUALITY_THRESHOLD = 0.45f;

    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final Map<String, RegistrationSession> sessions = new ConcurrentHashMap<>();

    public EnrollResponse registerPose(String userId, String pose, AiEmbeddingResult embeddingResult) {
        String normalizedPose = normalizePose(pose);
        cleanupExpiredSession(userId);
        validateQuality(normalizedPose, embeddingResult.getQualityScore());

        if ("front1".equals(normalizedPose)) {
            RegistrationSession session = RegistrationSession.start(userId, embeddingResult);
            sessions.put(userId, session);
            return buildResponse(userId, normalizedPose, embeddingResult);
        }

        RegistrationSession session = getActiveSession(userId);

        switch (normalizedPose) {
            case "front2", "front3" -> registerFrontPose(session, normalizedPose, embeddingResult);
            case "left", "right", "up", "down" -> registerProfilePose(session, normalizedPose, embeddingResult);
            default -> throw new FaceException(FaceErrorCode.INVALID_POSE);
        }

        return buildResponse(userId, normalizedPose, embeddingResult);
    }

    public boolean hasActiveSession(String userId) {
        cleanupExpiredSession(userId);
        return sessions.containsKey(userId);
    }

    public boolean isRegistrationReady(String userId) {
        cleanupExpiredSession(userId);
        RegistrationSession session = sessions.get(userId);
        return session != null && session.isComplete();
    }

    @Transactional
    public void persistPendingEmbeddings(String userId) {
        RegistrationSession session = getActiveSession(userId);
        if (!session.isComplete()) {
            throw new FaceException(FaceErrorCode.REGISTRATION_INCOMPLETE);
        }

        List<FaceEmbedding> entities = new ArrayList<>();
        for (String pose : REQUIRED_POSES) {
            float[] embedding = session.embeddingFor(pose);
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
            entities.add(entity);
        }
        faceEmbeddingRepository.saveAll(entities);
    }

    public void clearSession(String userId) {
        sessions.remove(userId);
    }

    private void registerFrontPose(RegistrationSession session, String pose, AiEmbeddingResult embeddingResult) {
        float similarity = cosineSimilarity(session.front1Embedding, embeddingResult.getEmbedding());
        if (similarity < FRONT_MATCH_THRESHOLD) {
            throw new FaceException(FaceErrorCode.REGISTRATION_MISMATCH);
        }
        session.putEmbedding(pose, embeddingResult.getEmbedding());
        session.rebuildAnchorIfReady();
    }

    private void registerProfilePose(RegistrationSession session, String pose, AiEmbeddingResult embeddingResult) {
        if (!session.hasAnchor()) {
            throw new FaceException(FaceErrorCode.REGISTRATION_FRONT_REQUIRED);
        }

        float threshold = ("up".equals(pose) || "down".equals(pose))
                ? VERTICAL_MATCH_THRESHOLD
                : SIDE_MATCH_THRESHOLD;

        float similarity = cosineSimilarity(session.anchorEmbedding, embeddingResult.getEmbedding());
        if (similarity < threshold) {
            throw new FaceException(FaceErrorCode.REGISTRATION_MISMATCH);
        }
        session.putEmbedding(pose, embeddingResult.getEmbedding());
    }

    private RegistrationSession getActiveSession(String userId) {
        RegistrationSession session = sessions.get(userId);
        if (session == null) {
            throw new FaceException(FaceErrorCode.REGISTRATION_SESSION_EXPIRED);
        }
        if (session.isExpired()) {
            sessions.remove(userId);
            throw new FaceException(FaceErrorCode.REGISTRATION_SESSION_EXPIRED);
        }
        return session;
    }

    private void cleanupExpiredSession(String userId) {
        RegistrationSession session = sessions.get(userId);
        if (session != null && session.isExpired()) {
            sessions.remove(userId);
        }
    }

    private void validateQuality(String pose, float qualityScore) {
        float threshold = switch (pose) {
            case "front1" -> FRONT1_QUALITY_THRESHOLD;
            case "front2", "front3" -> FRONT_QUALITY_THRESHOLD;
            default -> PROFILE_QUALITY_THRESHOLD;
        };
        if (qualityScore < threshold) {
            throw new FaceException(FaceErrorCode.REGISTRATION_QUALITY_LOW);
        }
    }

    private String normalizePose(String pose) {
        String normalizedPose = pose == null ? "" : pose.toLowerCase();
        if (!VALID_POSES.contains(normalizedPose)) {
            throw new FaceException(FaceErrorCode.INVALID_POSE);
        }
        return normalizedPose;
    }

    private EnrollResponse buildResponse(String userId, String pose, AiEmbeddingResult result) {
        return EnrollResponse.builder()
                .success(true)
                .userId(userId)
                .pose(pose)
                .savedAt(LocalDateTime.now())
                .aiProcessing(AiProcessingInfo.from(result))
                .build();
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

    private static class RegistrationSession {
        private final String userId;
        private final Map<String, float[]> embeddings = new ConcurrentHashMap<>();
        private final long expiresAtEpochSeconds;
        private final float[] front1Embedding;
        private float[] anchorEmbedding;

        private RegistrationSession(String userId, float[] front1Embedding, long expiresAtEpochSeconds) {
            this.userId = userId;
            this.front1Embedding = front1Embedding;
            this.expiresAtEpochSeconds = expiresAtEpochSeconds;
            this.embeddings.put("front1", front1Embedding);
        }

        static RegistrationSession start(String userId, AiEmbeddingResult result) {
            return new RegistrationSession(
                    userId,
                    result.getEmbedding(),
                    (System.currentTimeMillis() / 1000L) + SESSION_TTL_SECONDS
            );
        }

        boolean isExpired() {
            return (System.currentTimeMillis() / 1000L) > expiresAtEpochSeconds;
        }

        void putEmbedding(String pose, float[] embedding) {
            embeddings.put(pose, embedding);
        }

        boolean hasAnchor() {
            return anchorEmbedding != null;
        }

        void rebuildAnchorIfReady() {
            if (!embeddings.containsKey("front1") || !embeddings.containsKey("front2") || !embeddings.containsKey("front3")) {
                return;
            }
            float[] front1 = embeddings.get("front1");
            float[] front2 = embeddings.get("front2");
            float[] front3 = embeddings.get("front3");
            int len = Math.min(front1.length, Math.min(front2.length, front3.length));
            float[] avg = new float[len];
            for (int i = 0; i < len; i++) {
                avg[i] = (front1[i] + front2[i] + front3[i]) / 3f;
            }
            float norm = 0f;
            for (float value : avg) {
                norm += value * value;
            }
            if (norm == 0f) {
                this.anchorEmbedding = front1;
                return;
            }
            float scale = (float) Math.sqrt(norm);
            for (int i = 0; i < len; i++) {
                avg[i] = avg[i] / scale;
            }
            this.anchorEmbedding = avg;
        }

        boolean isComplete() {
            return embeddings.keySet().containsAll(REQUIRED_POSES);
        }

        float[] embeddingFor(String pose) {
            return Optional.ofNullable(embeddings.get(pose))
                    .orElseThrow(() -> new FaceException(FaceErrorCode.REGISTRATION_INCOMPLETE));
        }
    }
}
