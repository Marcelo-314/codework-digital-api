CREATE TABLE process_effort_clarification_continuation (
    id uuid NOT NULL,
    context_schema_version smallint NOT NULL,
    context_payload jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    CONSTRAINT pk_pecc PRIMARY KEY (id),
    CONSTRAINT ck_pecc_schema_version CHECK (context_schema_version = 1),
    CONSTRAINT ck_pecc_expires_after_created CHECK (expires_at > created_at)
);
