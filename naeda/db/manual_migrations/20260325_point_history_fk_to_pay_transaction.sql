-- point_history.payment_id FK를 payment → pay_transaction 으로 변경
-- 기존: payment(payment_id) 참조 → 신규: pay_transaction(id) 참조

ALTER TABLE point_history DROP CONSTRAINT IF EXISTS fk_point_history_payment;

ALTER TABLE point_history
    ADD CONSTRAINT fk_point_history_pay_transaction
        FOREIGN KEY (payment_id) REFERENCES pay_transaction (id) ON DELETE SET NULL;
