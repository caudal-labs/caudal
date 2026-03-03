CREATE TABLE IF NOT EXISTS workspaces (
    workspace_id  VARCHAR(255) PRIMARY KEY,
    api_key_hash  VARCHAR(255) NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS event_log (
    id              BIGSERIAL PRIMARY KEY,
    arrived_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    workspace_id    VARCHAR(255) NOT NULL DEFAULT 'default',
    space_id        VARCHAR(255) NOT NULL,
    event_timestamp VARCHAR(255),
    src             VARCHAR(1024) NOT NULL,
    dst             VARCHAR(1024) NOT NULL,
    weight          DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    type            VARCHAR(255),
    attrs           TEXT
);

CREATE INDEX idx_event_log_space ON event_log (space_id, arrived_at);
CREATE INDEX idx_event_log_workspace ON event_log (workspace_id, space_id, arrived_at);

CREATE TABLE IF NOT EXISTS snapshots (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  VARCHAR(255) NOT NULL DEFAULT 'default',
    space_id      VARCHAR(255) NOT NULL,
    bucket        BIGINT NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    payload       BYTEA NOT NULL,
    codec         VARCHAR(50) NOT NULL DEFAULT 'json+gzip',
    version       INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_snapshots_space ON snapshots (space_id, created_at DESC);
