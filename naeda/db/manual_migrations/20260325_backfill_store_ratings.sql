-- store.rating 백필용 수동 마이그레이션
-- 목적:
-- 1) rating 이 0 또는 NULL 인 매장에 데모용 평점을 채운다.
-- 2) is_recommended = true 인 매장은 더 높은 구간(4.2 ~ 5.0)을 사용한다.
-- 3) is_recommended 컬럼이 없는 DB 에서도 일반 구간(3.5 ~ 4.5)으로 안전하게 동작한다.

DO $$
DECLARE
    has_is_recommended BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'store'
          AND column_name = 'is_recommended'
    )
    INTO has_is_recommended;

    IF has_is_recommended THEN
        EXECUTE $sql$
            UPDATE store
            SET rating = CASE
                WHEN COALESCE(is_recommended, FALSE)
                    THEN ROUND((4.2 + random() * 0.8)::numeric, 1)::double precision
                ELSE ROUND((3.5 + random() * 1.0)::numeric, 1)::double precision
            END
            WHERE rating IS NULL OR rating = 0
        $sql$;
    ELSE
        UPDATE store
        SET rating = ROUND((3.5 + random() * 1.0)::numeric, 1)::double precision
        WHERE rating IS NULL OR rating = 0;

        RAISE NOTICE 'public.store.is_recommended column not found. Applied 3.5~4.5 range to all zero/null ratings.';
    END IF;
END $$;

-- 검증 예시
-- SELECT store_id, store_name, is_recommended, rating
-- FROM store
-- ORDER BY rating DESC, store_id ASC
-- LIMIT 50;
