-- user Table ----------------------

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(300) NOT NULL UNIQUE,
    password VARCHAR(500) NOT NULL,
    role VARCHAR(100) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTV',
    CONSTRAINT users_status_check CHECK (status IN ('ACTV', 'ICTV', 'DELE', 'BLOCKED'))
);

-- Index on status for filtering active users
CREATE INDEX idx_users_status ON users(status);

-- refresh_tokens Table ---------------

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(500) NOT NULL UNIQUE,
    fk_user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (fk_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(fk_user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);



CREATE TABLE IF NOT EXISTS broker_credentials (
    id BIGSERIAL PRIMARY KEY,
    fk_user_id BIGINT NOT NULL,
    broker_name VARCHAR(50) NOT NULL,
    broker_api_config JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTV',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_broker_user FOREIGN KEY (fk_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_broker_user UNIQUE (fk_user_id, broker_name),
	CONSTRAINT chk_broker_status CHECK (status IN ('ACTV', 'ICTV', 'DELE', 'BLOCKED'))
);


-- Index for fast lookup by user + status
CREATE INDEX idx_broker_user_status ON broker_credentials(fk_user_id, status);





