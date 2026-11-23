CREATE TABLE players
(
    player_id   UUID PRIMARY KEY,
    steam_id64  BIGINT      NOT NULL UNIQUE,
    player_name VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE auth_tokens
(
    token_hash VARCHAR(64) PRIMARY KEY,
    player_id  UUID        NOT NULL REFERENCES players (player_id),
    issued_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX auth_tokens_expires_at_idx ON auth_tokens (expires_at);

ALTER TABLE telemetry_events
    ADD COLUMN player_id UUID;

CREATE INDEX telemetry_events_player_received_idx ON telemetry_events (player_id, received_at);
