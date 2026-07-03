CREATE TABLE payment_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    checkout_request_id VARCHAR(64)  NOT NULL,
    merchant_request_id VARCHAR(64),
    phone_number        VARCHAR(15)  NOT NULL,
    amount              DECIMAL(12, 2) NOT NULL,
    account_reference   VARCHAR(32)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    result_code         INT,
    result_desc         VARCHAR(255),
    mpesa_receipt       VARCHAR(32),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_checkout_request UNIQUE (checkout_request_id)
);

CREATE INDEX idx_transactions_status ON payment_transactions (status);
CREATE INDEX idx_transactions_phone ON payment_transactions (phone_number);

CREATE TABLE callback_audit_log (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    checkout_request_id VARCHAR(64) NOT NULL,
    raw_payload         JSON        NOT NULL,
    processing_status   VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_checkout ON callback_audit_log (checkout_request_id);
