CREATE TABLE telemetry_events
(
    event_id          UUID PRIMARY KEY,
    schema_version    INTEGER      NOT NULL,
    name              VARCHAR(64)  NOT NULL,
    occurred_at       TIMESTAMPTZ  NOT NULL,
    received_at       TIMESTAMPTZ  NOT NULL,
    build_id          VARCHAR(64)  NOT NULL,
    installation_id   UUID         NOT NULL,
    session_id        UUID         NOT NULL,
    run_id            UUID         NOT NULL,
    attempt_id        UUID         NOT NULL,
    event_seq         BIGINT       NOT NULL,
    level_id          VARCHAR(64)  NOT NULL,
    mission_id        VARCHAR(64)  NOT NULL,
    active_elapsed_ms BIGINT       NOT NULL,
    x                 DOUBLE PRECISION,
    y                 DOUBLE PRECISION,
    z                 DOUBLE PRECISION,
    properties        JSONB        NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX telemetry_events_received_at_idx ON telemetry_events (received_at);
CREATE INDEX telemetry_events_attempt_idx ON telemetry_events (build_id, level_id, installation_id, attempt_id);
CREATE INDEX telemetry_events_session_seq_idx ON telemetry_events (run_id, attempt_id, build_id, session_id, event_seq);
CREATE INDEX telemetry_events_installation_received_idx ON telemetry_events (installation_id, received_at);
