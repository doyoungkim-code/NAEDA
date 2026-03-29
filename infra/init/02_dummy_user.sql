-- Swagger/개발 테스트용 더미 사용자 1건
-- 실행 후 /api/v1/face/enroll 의 userId 로 `dummy.user@naeda.local` 사용 가능
INSERT INTO "user" (
    user_id,
    password,
    username,
    resident_no,
    phone,
    institution_code,
    user_key,
    face_registered,
    created,
    modified
)
VALUES (
    'dummy.user@naeda.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '더미사용자',
    '0000000',
    '01099990000',
    'NAEDA',
    NULL,
    FALSE,
    NOW(),
    NOW()
)
ON CONFLICT (user_id) DO NOTHING;