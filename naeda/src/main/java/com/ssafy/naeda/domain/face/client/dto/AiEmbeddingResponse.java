package com.ssafy.naeda.domain.face.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

/**
 * AI 서버 POST /internal/v1/embeddings/extract 성공 응답
 * {
 *   "embedding": [0.12, -0.03, ...],  <- 512개 float
 *   "dim": 512,
 *   "model": "arcface-buffalo_l",
<<<<<<< HEAD
 *   "faceCount": 1
=======
 *   "faceCount": 1,
 *   "qualityScore": 0.95,
 *   "yaw": 1.2,
 *   "pitch": -0.8,
 *   "roll": 0.1
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
 * }
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiEmbeddingResponse {

    private List<Float> embedding;
    private int dim;
    private String model;
    private int faceCount;
<<<<<<< HEAD
=======
    private float qualityScore;
    private float yaw;
    private float pitch;
    private float roll;
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422

    public float[] toFloatArray() {
        float[] arr = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            arr[i] = embedding.get(i);
        }
        return arr;
    }
}
