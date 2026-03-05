-- face_embeddings 테이블 추가 (기존 DB 보정용)
CREATE TABLE IF NOT EXISTS face_embeddings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    pose VARCHAR(16) NOT NULL,
    embedding TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_face_embeddings_user_pose UNIQUE (user_id, pose),
    CONSTRAINT fk_face_embeddings_user_id FOREIGN KEY (user_id) REFERENCES "user" (user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_face_embeddings_user_id ON face_embeddings (user_id);
