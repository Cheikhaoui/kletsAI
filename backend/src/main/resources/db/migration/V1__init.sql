-- KletsAI initial schema. Every user-owned table carries user_id so multi-user is a later, non-breaking change.

CREATE TABLE app_user (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- The single user until login exists. Its id is configured as kletsai.single-user-id.
INSERT INTO app_user (id, display_name) VALUES ('00000000-0000-0000-0000-000000000001', 'Me');

CREATE TABLE conversation (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    scenario   VARCHAR(30)  NOT NULL CHECK (scenario IN ('FREE', 'TRAIN_TICKET', 'DOCTOR', 'COLLEAGUE')),
    title      VARCHAR(200),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_conversation_user ON conversation (user_id, created_at DESC);

CREATE TABLE chat_message (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    role            VARCHAR(10) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    text            TEXT        NOT NULL, -- user text, or assistant lines as JSON [{nl,en}]
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_message_conversation ON chat_message (conversation_id, created_at);

CREATE TABLE mistake (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    message_id     UUID        REFERENCES chat_message (id) ON DELETE SET NULL,
    original_text  TEXT        NOT NULL,
    corrected_text TEXT        NOT NULL,
    explanation_en TEXT,
    category       VARCHAR(30) NOT NULL CHECK (category IN
        ('WORD_ORDER', 'DE_HET', 'VERB_CONJUGATION', 'SPELLING', 'VOCABULARY', 'PREPOSITION', 'OTHER')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_mistake_user_category ON mistake (user_id, category);

CREATE TABLE card (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    card_type        VARCHAR(10)   NOT NULL DEFAULT 'WORD' CHECK (card_type IN ('WORD', 'SENTENCE')),
    dutch            VARCHAR(500)  NOT NULL,
    english          VARCHAR(500)  NOT NULL,
    article          VARCHAR(3)    CHECK (article IN ('DE', 'HET')),
    example_nl       VARCHAR(500),
    example_en       VARCHAR(500),
    source           VARCHAR(10)   NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'SEED', 'MISTAKE')),
    mistake_id       UUID          REFERENCES mistake (id) ON DELETE SET NULL,
    -- SM-2 state
    ease_factor      NUMERIC(4, 2) NOT NULL DEFAULT 2.50 CHECK (ease_factor >= 1.30),
    interval_days    INTEGER       NOT NULL DEFAULT 0,
    repetitions      INTEGER       NOT NULL DEFAULT 0,
    due_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_reviewed_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_card_user_type_dutch ON card (user_id, card_type, lower(dutch));
CREATE INDEX idx_card_user_due ON card (user_id, due_at);

CREATE TABLE review_log (
    id             UUID PRIMARY KEY, -- generated on the client so sync retries are idempotent
    user_id        UUID          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    card_id        UUID          NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    grade          SMALLINT      NOT NULL CHECK (grade BETWEEN 0 AND 3), -- 0=Again 1=Hard 2=Good 3=Easy
    reviewed_at    TIMESTAMPTZ   NOT NULL,                               -- device time
    synced_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    interval_after INTEGER       NOT NULL,
    ease_after     NUMERIC(4, 2) NOT NULL
);
CREATE INDEX idx_review_log_card ON review_log (card_id, reviewed_at);
