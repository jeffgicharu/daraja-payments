CREATE TABLE outbox_events (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(32)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    event_type     VARCHAR(32)  NOT NULL,
    payload        JSON         NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMP    NULL
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (published_at, id);
