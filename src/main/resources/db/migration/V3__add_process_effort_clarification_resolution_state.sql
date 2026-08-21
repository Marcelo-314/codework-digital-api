ALTER TABLE process_effort_clarification_continuation
    ADD COLUMN resolved_at timestamptz NULL;

ALTER TABLE process_effort_clarification_continuation
    ADD CONSTRAINT ck_pecc_resolved_within_active_lifetime
    CHECK (
        resolved_at IS NULL
        OR (
            resolved_at >= created_at
            AND resolved_at < expires_at
        )
    );
