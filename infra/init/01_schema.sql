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

CREATE TABLE "user" (
    user_no           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           VARCHAR(100)  NOT NULL UNIQUE,
    password          VARCHAR(255)  NOT NULL,
    username          VARCHAR(50)   NOT NULL,
    resident_no       VARCHAR(7)    NOT NULL,
    phone             VARCHAR(20)   NOT NULL UNIQUE,
    institution_code  VARCHAR(50)   NOT NULL,
    user_key          VARCHAR(255),
    face_registered   BOOLEAN       NOT NULL DEFAULT FALSE,
    pin_password      VARCHAR(255),
    created           TIMESTAMP     NOT NULL DEFAULT NOW(),
    modified          TIMESTAMP
);

CREATE TABLE admin (
    admin_no   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_id   VARCHAR(100),
    password   VARCHAR(255),
    adminname  VARCHAR(50)
);

CREATE TABLE face_vector (
    face_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no           BIGINT           NOT NULL,
    encrypted_vector  BYTEA            NOT NULL,
    angle_type        angle_type_enum  NOT NULL,
    tolerance         FLOAT            NOT NULL DEFAULT 0.6,
    registered        TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE face_embeddings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     VARCHAR(64)   NOT NULL,
    pose        VARCHAR(16)   NOT NULL,
    embedding   TEXT          NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_face_embeddings_user_pose UNIQUE (user_id, pose)
);

CREATE TABLE account (
    account_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no       BIGINT        NOT NULL,
    bank_code     VARCHAR(10),
    bank_name     VARCHAR(50),
    account_no    VARCHAR(50)   NOT NULL UNIQUE,
    account_name  VARCHAR(100),
    created       TIMESTAMP     DEFAULT NOW()
);

CREATE TABLE debit_card (
    debit_card_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no          BIGINT        NOT NULL,
    card_no          VARCHAR(50)   NOT NULL UNIQUE,
    cvc              VARCHAR(3)    NOT NULL,
    card_unique_no   VARCHAR(50)   NOT NULL,
    card_issuer_code VARCHAR(10)   NOT NULL,
    card_issuer_name VARCHAR(50)   NOT NULL,
    card_name        VARCHAR(100)  NOT NULL,
    card_expiry_date VARCHAR(8)    NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created          TIMESTAMP     NOT NULL DEFAULT NOW(),
    account_id       BIGINT        NOT NULL
);

CREATE TABLE credit_card (
    credit_card_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no          BIGINT        NOT NULL,
    card_no          VARCHAR(50)   NOT NULL UNIQUE,
    cvc              VARCHAR(3)    NOT NULL,
    card_unique_no   VARCHAR(50)   NOT NULL,
    card_issuer_code VARCHAR(10)   NOT NULL,
    card_issuer_name VARCHAR(50)   NOT NULL,
    card_name        VARCHAR(100)  NOT NULL,
    card_expiry_date VARCHAR(8)    NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    credit_limit     BIGINT        NOT NULL,
    billing_date     INT           NOT NULL,
    created          TIMESTAMP     NOT NULL DEFAULT NOW(),
    account_id       BIGINT        NOT NULL
);

CREATE TABLE payment_method (
    payment_method_id  BIGINT            GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no            BIGINT            NOT NULL,
    method_type        method_type_enum  NOT NULL,
    account_id         BIGINT,
    debit_card_id      BIGINT,
    credit_card_id     BIGINT,
    is_default         BOOLEAN           NOT NULL DEFAULT FALSE,
    is_face_pay        BOOLEAN           NOT NULL DEFAULT FALSE,
    created            TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE TABLE store (
    store_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no           BIGINT         NOT NULL,
    store_name        VARCHAR(100)   NOT NULL,
    category          VARCHAR(30)    NOT NULL,
    road_address      VARCHAR(255)   NOT NULL,
    number_address    VARCHAR(255),
    latitude          FLOAT,
    longitude         FLOAT,
    phone             VARCHAR(20),
    is_local_business BOOLEAN        NOT NULL DEFAULT FALSE,
    face_pay_enabled  BOOLEAN        NOT NULL DEFAULT FALSE,
    rating            FLOAT          NOT NULL DEFAULT 0,
    created           TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE payment (
    payment_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no              BIGINT                NOT NULL,
    store_id             BIGINT                NOT NULL,
    payment_method_id    BIGINT                NOT NULL,
    amount               BIGINT                NOT NULL,
    auth_method          auth_method_enum      NOT NULL,
    auth_level           auth_level_enum       NOT NULL,
    status               payment_status_enum   NOT NULL,
    face_distance        FLOAT,
    liveness_passed      BOOLEAN,
    pin_verified         BOOLEAN               NOT NULL DEFAULT FALSE,
    fds_score            INT                   NOT NULL DEFAULT 0,
    fds_action           fds_action_enum       NOT NULL DEFAULT 'NONE',
    earned_points        INT                   NOT NULL DEFAULT 0,
    ssafy_transaction_id VARCHAR(100),
    paid                 TIMESTAMP             NOT NULL DEFAULT NOW()
);

CREATE TABLE transaction_log (
    log_id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id           BIGINT                  NOT NULL,
    transaction_type     transaction_type_enum   NOT NULL,
    amount               BIGINT                  NOT NULL,
    balance_after        BIGINT                  NOT NULL,
    counterpart          VARCHAR(100),
    memo                 VARCHAR(255),
    category             VARCHAR(30),
    ssafy_transaction_id VARCHAR(100),
    transacted           TIMESTAMP               NOT NULL DEFAULT NOW()
);

CREATE TABLE point_wallet (
    wallet_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no      BIGINT    NOT NULL UNIQUE,
    balance      BIGINT    NOT NULL DEFAULT 0,
    total_earned BIGINT    NOT NULL DEFAULT 0,
    total_used   BIGINT    NOT NULL DEFAULT 0,
    updated      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE point_history (
    history_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wallet_id     BIGINT           NOT NULL,
    type          point_type_enum  NOT NULL,
    amount        BIGINT           NOT NULL,
    balance_after BIGINT           NOT NULL,
    description   VARCHAR(255),
    created       TIMESTAMP        NOT NULL DEFAULT NOW(),
    payment_id    BIGINT
);

CREATE TABLE point_product (
    product_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name    VARCHAR(120)              NOT NULL,
    description     VARCHAR(500),
    category        VARCHAR(50),
    image_url       VARCHAR(500),
    point_price     BIGINT                    NOT NULL,
    stock_quantity  INT                       NOT NULL,
    status          point_product_status_enum NOT NULL DEFAULT 'ON_SALE',
    starts_at       TIMESTAMP,
    ends_at         TIMESTAMP
);

CREATE TABLE point_order (
    order_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no        BIGINT        NOT NULL,
    product_id     BIGINT        NOT NULL,
    order_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    road_address   VARCHAR(255),
    number_address VARCHAR(255)
);

CREATE TABLE consumption_report (
    report_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no            BIGINT             NOT NULL,
    period_type        period_type_enum   NOT NULL,
    period_start       DATE               NOT NULL,
    period_end         DATE               NOT NULL,
    category_breakdown JSONB,
    total_spending     BIGINT,
    local_spending     BIGINT,
    local_ratio        FLOAT,
    local_grade        local_grade_enum,
    insights           JSONB,
    generated          TIMESTAMP          NOT NULL DEFAULT NOW()
);

CREATE TABLE fds_log (
    fds_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id      BIGINT           NOT NULL,
    user_no         BIGINT           NOT NULL,
    anomaly_score   INT              NOT NULL,
    triggered_rules JSONB,
    action_taken    fds_action_enum  NOT NULL,
    user_confirmed  BOOLEAN          NOT NULL DEFAULT FALSE,
    detected        TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE festival (
    festival_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title          VARCHAR(200)   NOT NULL,
    description    TEXT,
    location       VARCHAR(255),
    road_address   VARCHAR(255),
    number_address VARCHAR(255),
    latitude       FLOAT,
    longitude      FLOAT,
    link_url       VARCHAR(500),
    image_url      VARCHAR(500),
    start_date     DATE           NOT NULL,
    end_date       DATE           NOT NULL,
    fcm_notified   BOOLEAN        NOT NULL DEFAULT FALSE,
    created        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE notification (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no         BIGINT                  NOT NULL,
    type            notification_type_enum  NOT NULL,
    title           VARCHAR(200)            NOT NULL,
    body            TEXT,
    reference_id    BIGINT,
    reference_type  reference_type_enum,
    is_read         BOOLEAN                 NOT NULL DEFAULT FALSE,
    sent            TIMESTAMP               NOT NULL DEFAULT NOW()
);

-- ================================================
-- 3. FK 제약 조건
-- ================================================

ALTER TABLE face_vector ADD CONSTRAINT fk_face_vector_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE face_embeddings ADD CONSTRAINT fk_face_embeddings_user_id FOREIGN KEY (user_id) REFERENCES "user" (user_id) ON DELETE CASCADE;
ALTER TABLE account ADD CONSTRAINT fk_account_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE debit_card ADD CONSTRAINT fk_debit_card_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE debit_card ADD CONSTRAINT fk_debit_card_account FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE RESTRICT;
ALTER TABLE credit_card ADD CONSTRAINT fk_credit_card_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE credit_card ADD CONSTRAINT fk_credit_card_account FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE RESTRICT;
ALTER TABLE payment_method ADD CONSTRAINT fk_payment_method_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE payment_method ADD CONSTRAINT fk_payment_method_account FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE SET NULL;
ALTER TABLE payment_method ADD CONSTRAINT fk_payment_method_debit_card FOREIGN KEY (debit_card_id) REFERENCES debit_card (debit_card_id) ON DELETE SET NULL;
ALTER TABLE payment_method ADD CONSTRAINT fk_payment_method_credit_card FOREIGN KEY (credit_card_id) REFERENCES credit_card (credit_card_id) ON DELETE SET NULL;
ALTER TABLE store ADD CONSTRAINT fk_store_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE payment ADD CONSTRAINT fk_payment_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE payment ADD CONSTRAINT fk_payment_store FOREIGN KEY (store_id) REFERENCES store (store_id) ON DELETE RESTRICT;
ALTER TABLE payment ADD CONSTRAINT fk_payment_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method (payment_method_id) ON DELETE RESTRICT;
ALTER TABLE transaction_log ADD CONSTRAINT fk_transaction_log_account FOREIGN KEY (account_id) REFERENCES account (account_id) ON DELETE CASCADE;
ALTER TABLE point_wallet ADD CONSTRAINT fk_point_wallet_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE point_history ADD CONSTRAINT fk_point_history_wallet FOREIGN KEY (wallet_id) REFERENCES point_wallet (wallet_id) ON DELETE CASCADE;
ALTER TABLE point_history ADD CONSTRAINT fk_point_history_payment FOREIGN KEY (payment_id) REFERENCES payment (payment_id) ON DELETE SET NULL;
ALTER TABLE point_order ADD CONSTRAINT fk_point_order_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE point_order ADD CONSTRAINT fk_point_order_product FOREIGN KEY (product_id) REFERENCES point_product (product_id) ON DELETE RESTRICT;
ALTER TABLE consumption_report ADD CONSTRAINT fk_consumption_report_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE fds_log ADD CONSTRAINT fk_fds_log_payment FOREIGN KEY (payment_id) REFERENCES payment (payment_id) ON DELETE CASCADE;
ALTER TABLE fds_log ADD CONSTRAINT fk_fds_log_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;
ALTER TABLE notification ADD CONSTRAINT fk_notification_user FOREIGN KEY (user_no) REFERENCES "user" (user_no) ON DELETE CASCADE;