CREATE TABLE users (
    id              UUID          NOT NULL PRIMARY KEY,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    user_name       VARCHAR(50)   NOT NULL,
    hashed_password VARCHAR(255)  NOT NULL,
    cred_created_at TIMESTAMPTZ   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
