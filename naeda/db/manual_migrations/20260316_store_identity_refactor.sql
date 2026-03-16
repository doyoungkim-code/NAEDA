-- 기존 store 테이블을 내부 PK(store_id) + SSAFY 외부 식별자(ssafy_merchant_id) 구조로 전환한다.
-- 대상: 이미 운영/개발 중인 PostgreSQL DB (기존 볼륨 유지)

ALTER TABLE store
    ADD COLUMN IF NOT EXISTS ssafy_merchant_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) NOT NULL DEFAULT 'SSAFY',
    ADD COLUMN IF NOT EXISTS source_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_enriched_at TIMESTAMP;

ALTER TABLE store
    ALTER COLUMN user_no DROP NOT NULL,
    ALTER COLUMN road_address DROP NOT NULL,
    ALTER COLUMN is_local_business SET DEFAULT FALSE,
    ALTER COLUMN face_pay_enabled SET DEFAULT FALSE,
    ALTER COLUMN rating SET DEFAULT 0;

UPDATE store
SET source_type = 'SSAFY'
WHERE source_type IS NULL;

UPDATE store
SET is_active = TRUE
WHERE is_active IS NULL;

UPDATE store
SET ssafy_merchant_id = store_id
WHERE ssafy_merchant_id IS NULL
  AND COALESCE(source_type, 'SSAFY') = 'SSAFY';

UPDATE store
SET source_key = 'ssafy:' || ssafy_merchant_id::TEXT
WHERE source_key IS NULL
  AND ssafy_merchant_id IS NOT NULL
  AND COALESCE(source_type, 'SSAFY') = 'SSAFY';

CREATE SEQUENCE IF NOT EXISTS store_store_id_seq;

SELECT setval(
    'store_store_id_seq',
    COALESCE((SELECT MAX(store_id) FROM store), 1),
    TRUE
);

ALTER SEQUENCE store_store_id_seq OWNED BY store.store_id;

ALTER TABLE store
    ALTER COLUMN store_id SET DEFAULT nextval('store_store_id_seq');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_store_ssafy_merchant_id'
    ) THEN
        ALTER TABLE store
            ADD CONSTRAINT uk_store_ssafy_merchant_id UNIQUE (ssafy_merchant_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_store_source_key'
    ) THEN
        ALTER TABLE store
            ADD CONSTRAINT uk_store_source_key UNIQUE (source_key);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS store_seed_metadata (
    seed_key     VARCHAR(100) PRIMARY KEY,
    content_hash VARCHAR(64)  NOT NULL,
    updated      TIMESTAMP    NOT NULL DEFAULT NOW()
);
