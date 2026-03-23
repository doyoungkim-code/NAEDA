-- 공공 매장 대표 이미지 재보강용 수동 마이그레이션
-- 목적:
-- 1) 기존에 저장된 목업 이미지 URL(/images/store/default-*.svg)을 제거한다.
-- 2) last_enriched_at을 null로 되돌려 다음 기동 시 전체 재보강 대상에 포함시킨다.
--
-- 전제:
-- - 최신 보강 코드(map-v6)가 배포된 상태여야 한다.
-- - 백엔드 재시작 시 StoreCatalogBootstrapRunner가 다시 실행된다.
--
-- 주의:
-- - source_type = PUBLIC_CSV, is_active = true 인 공공 매장만 대상으로 한다.
-- - 네이버 접근이 서버에서 실패하면 image_url은 null로 남을 수 있다.

UPDATE store
SET image_url = NULL,
    last_enriched_at = NULL
WHERE source_type = 'PUBLIC_CSV'
  AND is_active = true
  AND image_url LIKE '/images/store/default-%';
