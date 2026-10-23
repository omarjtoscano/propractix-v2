-- Technical owner: platform.ratelimit. Ephemeral abuse-prevention pseudonyms only.
CREATE TABLE platform_rate_limit_bucket (
    operation_id varchar(128) NOT NULL,
    policy_version integer NOT NULL CHECK (policy_version > 0),
    rule_id varchar(64) NOT NULL CHECK (rule_id ~ '^[a-z][a-z0-9_]{0,63}$'),
    hmac_key_version varchar(64) NOT NULL CHECK (hmac_key_version ~ '^[a-z][a-z0-9-]{0,63}$'),
    subject_fingerprint varchar(43) NOT NULL CHECK (subject_fingerprint ~ '^[A-Za-z0-9_-]{43}$'),
    window_started_at timestamptz NOT NULL,
    request_count integer NOT NULL CHECK (request_count > 0),
    expires_at timestamptz NOT NULL CHECK (expires_at > window_started_at),
    PRIMARY KEY (operation_id, policy_version, rule_id, hmac_key_version,
                 subject_fingerprint, window_started_at)
);

CREATE INDEX platform_rate_limit_bucket_expiry_idx ON platform_rate_limit_bucket (expires_at);
