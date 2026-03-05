-- face_embeddings 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_face_embeddings_user_id ON face_embeddings (user_id);