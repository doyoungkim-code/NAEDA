package com.ssafy.naeda.domain.face.repository;

import com.ssafy.naeda.domain.face.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, Long> {

    Optional<FaceEmbedding> findByUserIdAndPose(String userId, String pose);

    List<FaceEmbedding> findAll();

    void deleteByUserId(String userId);
}
