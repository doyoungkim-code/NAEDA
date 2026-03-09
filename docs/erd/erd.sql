-- ================================================
-- 내다(NaeDa) ERD - PostgreSQL DDL
-- ================================================

-- ================================================
-- 1. ENUM 타입 정의
-- ================================================
CREATE TYPE transaction_type_enum AS ENUM ('DEPOSIT', 'WITHDRAW', 'TRANSFER');
CREATE TYPE period_type_enum AS ENUM ('WEEKLY', 'MONTHLY');
CREATE TYPE local_grade_enum AS ENUM ('A', 'B', 'C', 'D');
CREATE TYPE auth_method_enum AS ENUM ('FACE_PAY', 'PIN_FALLBACK');
CREATE TYPE auth_level_enum AS ENUM ('FACE_ONLY', 'FACE_PHONE', 'FACE_PIN', 'FACE_SIGNATURE', 'BLOCKED');
CREATE TYPE payment_status_enum AS ENUM ('SUCCESS', 'FAILED', 'CANCELLED', 'BLOCKED');
CREATE TYPE fds_action_enum AS ENUM ('NONE', 'ALERT', 'PAUSE', 'BLOCK');
CREATE TYPE point_type_enum AS ENUM ('EARN', 'USE_COUPON');
CREATE TYPE angle_type_enum AS ENUM ('front1', 'front2', 'front3', 'left', 'right', 'down', 'up');
CREATE TYPE point_product_status_enum AS ENUM ('ON_SALE', 'SOLD_OUT');
CREATE TYPE method_type_enum AS ENUM ('ACCOUNT', 'DEBIT_CARD', 'CREDIT_CARD');
CREATE TYPE notification_type_enum AS ENUM ('PAYMENT', 'FDS_ALERT', 'FESTIVAL', 'POINT', 'SYSTEM');
CREATE TYPE reference_type_enum AS ENUM ('PAYMENT', 'FESTIVAL');

-- ================================================
-- 2. 테이블 생성
-- ================================================

-- 1) User (회원 정보)
CREATE TABLE "user" (
    user_no           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           VARCHAR(100)  NOT NULL UNIQUE,       -- 이메일 (로그인)
    password          VARCHAR(255)  NOT NULL,               -- 비밀번호 (BCrypt)
    username          VARCHAR(50)   NOT NULL,               -- 이름
    resident_no       VARCHAR(7)    NOT NULL,               -- 주민등록번호 앞 7자리
    phone             VARCHAR(20)   NOT NULL UNIQUE,        -- 전화번호
    institution_code  VARCHAR(50)   NOT NULL,               -- 기관코드
    user_key          VARCHAR(255),                         -- SSAFY API 유저 키
    face_registered   BOOLEAN       NOT NULL DEFAULT FALSE, -- 얼굴 등록 여부
    pin_password      VARCHAR(255),                         -- 6자리 Pin 비밀번호 (BCrypt)
    created           TIMESTAMP     NOT NULL DEFAULT NOW(),
    modified          TIMESTAMP
);

-- 2) Admin (관리자)
CREATE TABLE admin (
    admin_no   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_id   VARCHAR(100),                                -- 관리자 ID
    password   VARCHAR(255),                                -- 비밀번호
    adminname  VARCHAR(50)                                  -- 관리자명
);

-- 3) Face_Vector (암호화된 얼굴 벡터)
CREATE TABLE face_vector (
    face_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no           BIGINT           NOT NULL,               -- FK → user
    encrypted_vector  BYTEA            NOT NULL,               -- 128차원 벡터 (AES 암호화)
    angle_type        angle_type_enum  NOT NULL,               -- 촬영 각도 (front1/front2/front3/left/right/down/up)
    tolerance         FLOAT            NOT NULL DEFAULT 0.6,   -- 개인별 최적 임계값
    registered        TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- 4) Account (SSAFY 계좌)
CREATE TABLE account (
    account_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no       BIGINT        NOT NULL,               -- FK → user
    bank_code     VARCHAR(10),                           -- 은행 코드
    bank_name     VARCHAR(50),                           -- 은행명
    account_no    VARCHAR(50)   NOT NULL UNIQUE,         -- SSAFY 계좌번호
    account_name  VARCHAR(100),                          -- 계좌 별명
    created       TIMESTAMP     DEFAULT NOW()
);

-- 5) Debit_Card (체크카드)
CREATE TABLE debit_card (
    debit_card_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no        BIGINT        NOT NULL,                -- FK → user
    card_no        VARCHAR(50)   NOT NULL UNIQUE,         -- 카드번호
    cvc            VARCHAR(3)    NOT NULL,                -- cvc 번호
    card_unique_no VARCHAR(50)    NOT NULL,               -- 카드 고유번호
    card_issuer_code VARCHAR(10)    NOT NULL,             -- 카드사 코드
    card_issuer_name VARCHAR(50)    NOT NULL,             -- 카드사명
    card_name      VARCHAR(100)    NOT NULL,              -- 카드 상품명
    card_expiry_date VARCHAR(8)    NOT NULL,              -- 카드 만료일
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,   -- 활성 상태
    created        TIMESTAMP     NOT NULL DEFAULT NOW(),
    account_id     BIGINT        NOT NULL                 -- FK → account (연결 계좌)
);

-- 6) Credit_Card (신용카드)
CREATE TABLE credit_card (
    credit_card_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no         BIGINT        NOT NULL,                -- FK → user
    card_no         VARCHAR(50)   NOT NULL UNIQUE,         -- 카드번호
    cvc            VARCHAR(3)    NOT NULL,                -- cvc 번호
    card_unique_no VARCHAR(50)    NOT NULL,               -- 카드 고유번호
    card_issuer_code VARCHAR(10)    NOT NULL,             -- 카드사 코드
    card_issuer_name VARCHAR(50)    NOT NULL,             -- 카드사명
    card_name      VARCHAR(100)    NOT NULL,              -- 카드 상품명
    card_expiry_date VARCHAR(8)    NOT NULL,              -- 카드 만료일
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,   -- 활성 상태
    credit_limit    BIGINT        NOT NULL,                -- 신용 한도
    billing_date    INT           NOT NULL,                -- 결제일
    created         TIMESTAMP     NOT NULL DEFAULT NOW(),
    account_id      BIGINT        NOT NULL                 -- FK → account (출금 연결계좌)
);

-- 7) Payment_Method (결제 수단)
CREATE TABLE payment_method (
    payment_method_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no            BIGINT            NOT NULL,                -- FK → user
    method_type        method_type_enum  NOT NULL,                -- ACCOUNT / DEBIT_CARD / CREDIT_CARD
    account_id         BIGINT,                                    -- FK → account (계좌 결제 시)
    debit_card_id      BIGINT,                                    -- FK → debit_card (체크카드 결제 시)
    credit_card_id     BIGINT,                                    -- FK → credit_card (신용카드 결제 시)
    is_default         BOOLEAN           NOT NULL DEFAULT FALSE,  -- 기본 결제 수단 여부
    is_face_pay        BOOLEAN           NOT NULL DEFAULT FALSE,  -- 페이스페이 결제용 여부
    created            TIMESTAMP         NOT NULL DEFAULT NOW()
);

-- 8) Store (매장 정보)
CREATE TABLE store (
    store_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no           BIGINT         NOT NULL,                -- FK → user (사장님)
    account_id        BIGINT,                                 -- FK → account (정산 계좌)
    store_name        VARCHAR(100)   NOT NULL,                -- 매장명
    category          VARCHAR(30)    NOT NULL,                -- 한식/양식/카페/편의점 등
    road_address      VARCHAR(255)   NOT NULL,                -- 도로명 주소
    number_address    VARCHAR(255),                           -- 지번 주소
    latitude          FLOAT,                                  -- 위도
    longitude         FLOAT,                                  -- 경도
    phone             VARCHAR(20),                            -- 매장 전화번호
    is_local_business BOOLEAN        NOT NULL DEFAULT FALSE,  -- 구미 소상공인 여부 (포인트 2배)
    face_pay_enabled  BOOLEAN        NOT NULL DEFAULT FALSE,  -- 페이스페이 지원 여부
    rating            FLOAT          NOT NULL DEFAULT 0,      -- 평점
    created           TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 9) Payment (결제 내역)
CREATE TABLE payment (
    payment_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no              BIGINT                NOT NULL,              -- FK → user
    store_id             BIGINT                NOT NULL,              -- FK → store
    payment_method_id    BIGINT                NOT NULL,              -- FK → payment_method
    amount               BIGINT                NOT NULL,              -- 결제 금액
    auth_method          auth_method_enum      NOT NULL,              -- FACE_PAY / PIN_FALLBACK
    auth_level           auth_level_enum       NOT NULL,              -- FACE_ONLY / FACE_PHONE / FACE_PIN / FACE_SIGNATURE / BLOCKED
    status               payment_status_enum   NOT NULL,              -- SUCCESS / FAILED / CANCELLED / BLOCKED
    face_distance        FLOAT,                                       -- 얼굴 매칭 거리값 (낮을수록 유사)
    liveness_passed      BOOLEAN,                                     -- Liveness 통과 여부
    pin_verified         BOOLEAN               NOT NULL DEFAULT FALSE,-- 비밀번호 인증 여부
    fds_score            INT                   NOT NULL DEFAULT 0,    -- FDS 이상 점수 (0~100)
    fds_action           fds_action_enum       NOT NULL DEFAULT 'NONE',
    earned_points        INT                   NOT NULL DEFAULT 0,    -- 적립된 포인트
    ssafy_transaction_id VARCHAR(100),                                -- SSAFY 거래 ID
    paid                 TIMESTAMP             NOT NULL DEFAULT NOW()
);

-- 10) Transaction_Log (계좌 거래 내역)
CREATE TABLE transaction_log (
    log_id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id           BIGINT                  NOT NULL,            -- FK → account
    transaction_type     transaction_type_enum   NOT NULL,            -- DEPOSIT / WITHDRAW / TRANSFER
    amount               BIGINT                  NOT NULL,            -- 거래 금액
    balance_after        BIGINT                  NOT NULL,            -- 거래 후 잔액
    counterpart          VARCHAR(100),                                -- 상대방 (이체 시)
    memo                 VARCHAR(255),                                -- AI 분류 태그
    category             VARCHAR(30),                                 -- 식비/카페/교통/쇼핑 등 (AI 자동 분류)
    ssafy_transaction_id VARCHAR(100),                                -- SSAFY 거래 ID
    transacted           TIMESTAMP               NOT NULL DEFAULT NOW()
);

-- 11) Point_Wallet (포인트 잔액)
CREATE TABLE point_wallet (
    wallet_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no      BIGINT    NOT NULL UNIQUE,              -- FK → user (1인 1지갑)
    balance      BIGINT    NOT NULL DEFAULT 0,            -- 포인트 잔액
    total_earned BIGINT    NOT NULL DEFAULT 0,            -- 총 적립
    total_used   BIGINT    NOT NULL DEFAULT 0,            -- 총 사용
    updated      TIMESTAMP DEFAULT NOW()
);

-- 12) Point_History (포인트 적립/사용 이력)
CREATE TABLE point_history (
    history_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wallet_id     BIGINT           NOT NULL,              -- FK → point_wallet
    type          point_type_enum  NOT NULL,              -- EARN / USE_COUPON
    amount        BIGINT           NOT NULL,              -- 포인트 양
    balance_after BIGINT           NOT NULL,              -- 거래 후 잔액
    description   VARCHAR(255),                           -- 적립/사용 사유
    created       TIMESTAMP        NOT NULL DEFAULT NOW(),
    payment_id    BIGINT                                  -- FK → payment (적립 시 연결된 결제, NULL 허용)
);

-- 13) Point_Product (포인트 상품)
CREATE TABLE point_product (
    product_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name    VARCHAR(120)              NOT NULL,                -- 상품명
    description     VARCHAR(500),                                     -- 상품 설명
    category        VARCHAR(50),                                      -- 상품 카테고리
    image_url       VARCHAR(500),                                     -- 이미지 URL
    point_price     BIGINT                    NOT NULL,                -- 포인트 가격
    stock_quantity  INT                       NOT NULL,                -- 재고 수량
    status          point_product_status_enum NOT NULL DEFAULT 'ON_SALE', -- ON_SALE / SOLD_OUT
    starts_at       TIMESTAMP,                                        -- 판매 시작일
    ends_at         TIMESTAMP                                         -- 판매 종료일
);

-- 14) Point_Order (포인트 상품 주문)
CREATE TABLE point_order (
    order_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no     BIGINT        NOT NULL,                -- FK → user
    product_id  BIGINT        NOT NULL,                -- FK → point_product
    order_at    TIMESTAMP     NOT NULL DEFAULT NOW(),   -- 주문 시각
    road_address      VARCHAR(255),                    -- 도로명 주소
    number_address    VARCHAR(255)                     -- 지번 주소
);

-- 15) Consumption_Report (AI 소비 분석 리포트)
CREATE TABLE consumption_report (
    report_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no            BIGINT             NOT NULL,              -- FK → user
    period_type        period_type_enum   NOT NULL,              -- WEEKLY / MONTHLY
    period_start       DATE               NOT NULL,              -- 분석 기간 시작
    period_end         DATE               NOT NULL,              -- 분석 기간 끝
    category_breakdown JSONB,                                    -- {"식비": 320000, "카페": 85000, ...}
    total_spending     BIGINT,                                   -- 총 지출
    local_spending     BIGINT,                                   -- 구미 지역 지출
    local_ratio        FLOAT,                                    -- 지역 소비 비율 (0.0~1.0)
    local_grade        local_grade_enum,                         -- A / B / C / D (지역 기여 등급)
    insights           JSONB,                                    -- AI 절약 인사이트 배열
    generated          TIMESTAMP          NOT NULL DEFAULT NOW()
);

-- 16) FDS_Log (FDS 이상 거래 탐지 로그)
CREATE TABLE fds_log (
    fds_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id      BIGINT           NOT NULL,              -- FK → payment
    user_no         BIGINT           NOT NULL,              -- FK → user
    anomaly_score   INT              NOT NULL,              -- 이상 점수 (0~100)
    triggered_rules JSONB,                                  -- 발동된 규칙 배열
    action_taken    fds_action_enum  NOT NULL,              -- NONE / ALERT / PAUSE / BLOCK
    user_confirmed  BOOLEAN          NOT NULL DEFAULT FALSE,-- 사용자 본인 확인 여부
    detected        TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- 17) Festival (축제/이벤트 정보)
CREATE TABLE festival (
    festival_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title          VARCHAR(200)   NOT NULL,                -- 축제/이벤트명
    description    TEXT,                                   -- 소개 내용
    location       VARCHAR(255),                           -- 장소명
    road_address   VARCHAR(255),                           -- 도로명 주소
    number_address VARCHAR(255),                           -- 지번 주소
    latitude       FLOAT,                                  -- 위도
    longitude      FLOAT,                                  -- 경도
    link_url       VARCHAR(500),                           -- 링크 URL
    image_url      VARCHAR(500),                           -- 이미지 URL
    start_date     DATE           NOT NULL,                -- 시작일
    end_date       DATE           NOT NULL,                -- 종료일
    fcm_notified   BOOLEAN        NOT NULL DEFAULT FALSE,  -- FCM 공지 발송 여부
    created        TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 18) Notification (FCM 알림 이력)
CREATE TABLE notification (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no         BIGINT                  NOT NULL,              -- FK → user
    type            notification_type_enum  NOT NULL,              -- PAYMENT / FDS_ALERT / FESTIVAL / POINT / SYSTEM
    title           VARCHAR(200)            NOT NULL,              -- 알림 제목
    body            TEXT,                                          -- 알림 본문
    reference_id    BIGINT,                                        -- 관련 ID (결제/축제 등)
    reference_type  reference_type_enum,                           -- PAYMENT / FESTIVAL
    is_read         BOOLEAN                 NOT NULL DEFAULT FALSE,-- 읽음 여부
    sent            TIMESTAMP               NOT NULL DEFAULT NOW()
);

-- ================================================
-- 3. FK 제약 조건
-- ================================================

-- Face_Vector → User
ALTER TABLE face_vector
    ADD CONSTRAINT fk_face_vector_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Account → User
ALTER TABLE account
    ADD CONSTRAINT fk_account_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Debit_Card → User
ALTER TABLE debit_card
    ADD CONSTRAINT fk_debit_card_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Debit_Card → Account
ALTER TABLE debit_card
    ADD CONSTRAINT fk_debit_card_account
    FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE RESTRICT;

-- Credit_Card → User
ALTER TABLE credit_card
    ADD CONSTRAINT fk_credit_card_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Credit_Card → Account
ALTER TABLE credit_card
    ADD CONSTRAINT fk_credit_card_account
    FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE RESTRICT;

-- Payment_Method → User
ALTER TABLE payment_method
    ADD CONSTRAINT fk_payment_method_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Payment_Method → Account
ALTER TABLE payment_method
    ADD CONSTRAINT fk_payment_method_account
    FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE SET NULL;

-- Payment_Method → Debit_Card
ALTER TABLE payment_method
    ADD CONSTRAINT fk_payment_method_debit_card
    FOREIGN KEY (debit_card_id) REFERENCES debit_card (debit_card_id) ON DELETE SET NULL;

-- Payment_Method → Credit_Card
ALTER TABLE payment_method
    ADD CONSTRAINT fk_payment_method_credit_card
    FOREIGN KEY (credit_card_id) REFERENCES credit_card (credit_card_id) ON DELETE SET NULL;

-- Store → User
ALTER TABLE store
    ADD CONSTRAINT fk_store_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Store → Account
ALTER TABLE store
    ADD CONSTRAINT fk_store_account
    FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE SET NULL;

-- Payment → User
ALTER TABLE payment
    ADD CONSTRAINT fk_payment_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Payment → Store
ALTER TABLE payment
    ADD CONSTRAINT fk_payment_store
    FOREIGN KEY (store_id) REFERENCES store (store_id) ON DELETE RESTRICT;

-- Payment → Payment_Method
ALTER TABLE payment
    ADD CONSTRAINT fk_payment_payment_method
    FOREIGN KEY (payment_method_id) REFERENCES payment_method (payment_method_id) ON DELETE RESTRICT;

-- Transaction_Log → Account
ALTER TABLE transaction_log
    ADD CONSTRAINT fk_transaction_log_account
    FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE CASCADE;

-- Point_Wallet → User
ALTER TABLE point_wallet
    ADD CONSTRAINT fk_point_wallet_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Point_History → Point_Wallet
ALTER TABLE point_history
    ADD CONSTRAINT fk_point_history_wallet
    FOREIGN KEY (wallet_id) REFERENCES point_wallet (wallet_id) ON DELETE CASCADE;

-- Point_History → Payment
ALTER TABLE point_history
    ADD CONSTRAINT fk_point_history_payment
    FOREIGN KEY (payment_id) REFERENCES payment (payment_id) ON DELETE SET NULL;

-- Point_Product (독립 테이블, FK 없음)

-- Point_Order → User
ALTER TABLE point_order
    ADD CONSTRAINT fk_point_order_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Point_Order → Point_Product
ALTER TABLE point_order
    ADD CONSTRAINT fk_point_order_product
    FOREIGN KEY (product_id) REFERENCES point_product (product_id) ON DELETE RESTRICT;

-- Consumption_Report → User
ALTER TABLE consumption_report
    ADD CONSTRAINT fk_consumption_report_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- FDS_Log → Payment
ALTER TABLE fds_log
    ADD CONSTRAINT fk_fds_log_payment
    FOREIGN KEY (payment_id) REFERENCES payment (payment_id) ON DELETE CASCADE;

-- FDS_Log → User
ALTER TABLE fds_log
    ADD CONSTRAINT fk_fds_log_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;

-- Notification → User
ALTER TABLE notification
    ADD CONSTRAINT fk_notification_user
    FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
