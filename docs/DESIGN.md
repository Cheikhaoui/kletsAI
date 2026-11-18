# KletsAI: design

A personal Dutch tutor (A1 → A2), used mostly on a phone on the train.

## 1. Project structure

```
kletsAI/
├── CLAUDE.md
├── docker-compose.yml            # Postgres 16 only
├── .env.example                  # copy to .env, never commit .env
├── docs/DESIGN.md
├── backend/                      # Spring Boot 4.1, Java 21, Maven wrapper, Dockerfile
│   └── src/main/java/com/kletsai/
│       ├── KletsAiApplication.java
│       ├── common/               # AppUser, CurrentUser, /api/me, error handling
│       ├── flashcard/            # Card entity, CardService, CardController
│       ├── review/               # Sm2Scheduler (pure), ReviewService, sync endpoint
│       ├── chat/                 # Conversation, ChatMessage, Scenario, ChatController
│       ├── ai/                   # TutorClient (the only Claude caller), prompts, response DTOs
│       └── mistake/              # Mistake entity, MistakeService (save + make card)
│   └── src/main/resources/
│       ├── application.yml
│       ├── prompts/tutor-system.md
│       └── db/migration/V1__init.sql …
└── frontend/                     # Angular (latest), standalone components, PWA
    └── src/app/
        ├── core/                 # api services, offline queue (IndexedDB), models
        ├── features/
        │   ├── review/           # one card per screen, Again/Hard/Good/Easy
        │   ├── cards/            # list / add / edit cards
        │   ├── chat/             # scenario picker + chat screen
        │   └── mistakes/         # mistake history, counts per category
        └── shared/               # big-button UI pieces
```

Backend packages are split **by feature**, not by layer.

## 2. Database tables (PostgreSQL, Flyway)

Every user-owned table has `user_id`. For now one user row is seeded with a fixed UUID, and
`CurrentUser` always returns it. Adding login later means changing `CurrentUser`; the tables stay the same.

| Table | Columns |
|---|---|
| `app_user` | `id uuid pk`, `display_name`, `created_at` |
| `card` | `id uuid pk`, `user_id fk`, `card_type` (`WORD` \| `SENTENCE`), `dutch`, `english`, `article` (`DE` \| `HET` \| null), `example_nl`, `example_en`, `source` (`MANUAL` \| `SEED` \| `MISTAKE`), `mistake_id fk null`, **SM-2 state:** `ease_factor numeric(4,2) default 2.50`, `interval_days int default 0`, `repetitions int default 0`, `due_at timestamptz`, `last_reviewed_at`, plus `created_at`, `updated_at`. Unique `(user_id, card_type, lower(dutch))` |
| `review_log` | `id uuid pk` (**created by the client**, so a sync can be repeated safely), `user_id`, `card_id fk`, `grade smallint` (0=Again 1=Hard 2=Good 3=Easy), `reviewed_at` (time on the device), `synced_at`, `interval_after`, `ease_after` |
| `conversation` | `id uuid pk`, `user_id`, `scenario` (`FREE` \| `TRAIN_TICKET` \| `DOCTOR` \| `COLLEAGUE`), `title`, `created_at` |
| `chat_message` | `id uuid pk`, `conversation_id fk`, `role` (`USER` \| `ASSISTANT`), `text` (the user's text, or the tutor's full JSON reply: `{reply:[{nl,en}], corrections:[…]}`), `created_at` |
| `mistake` | `id uuid pk`, `user_id`, `message_id fk` (the user message), `original_text`, `corrected_text`, `explanation_en`, `category` (`WORD_ORDER` \| `DE_HET` \| `VERB_CONJUGATION` \| `SPELLING` \| `VOCABULARY` \| `PREPOSITION` \| `OTHER`), `created_at` |

Scenarios live in a Java enum, with each scenario's prompt text kept there. They are not stored in a table.

## 3. REST endpoints (`/api`)

**Flashcards**
- `GET    /cards` lists all cards (filter with `?source=MISTAKE`)
- `POST   /cards`, `PUT /cards/{id}`, `DELETE /cards/{id}`
- `GET    /cards/review-deck?days=2` returns cards due now **and** in the next N days, so a train ride without signal still has cards
- `GET    /cards/stats` returns due today, total, and reviewed today

**Reviews (offline-first)**
- `POST /reviews/sync` takes `[{id, cardId, grade, reviewedAt}]`. The server sorts by `reviewedAt`, skips ids it has already seen, runs SM-2, and returns the updated cards.

**Chat**
- `GET  /scenarios`
- `POST /conversations` with `{scenario}` returns the conversation and the tutor's first message
- `GET  /conversations`, `GET /conversations/{id}` (includes the messages)
- `POST /conversations/{id}/messages` with `{text}` returns `{learner, tutor}`: the learner message with its `corrections`, and the tutor message with its `lines`. A Claude failure returns 503 and nothing is saved

**Mistakes**
- `GET /mistakes?category=…`
- `GET /mistakes/stats` returns counts per category, which shows what to practise

## 4. Key design decisions

**SM-2 runs only on the server.** Offline, the client grades cards from the downloaded deck. An "Again" card
comes back later in the same session. The grades go into an IndexedDB queue and sync when the phone is back
online. This way SM-2 is written once, in Java, and tested in one place. Grade → SM-2 quality mapping:
Again=1 (resets repetitions), Hard=3, Good=4, Easy=5.

**One Claude call per chat turn returns structured JSON** (structured outputs), shaped like
`{reply: [{nl, en}], corrections: [{original, corrected, explanationEn, category, card?}]}`.
The UI shows the Dutch lines with the English under each one, so the format can't drift. Mistake extraction
is then plain Java: validate the corrections, drop empty or duplicate ones, save them as `mistake` rows,
and turn each one into a card. A `WORD` card if the AI suggested a word (for example a de/het noun),
otherwise a `SENTENCE` card. The model call stays behind `TutorClient`, so the extraction logic is tested
with fixed JSON and makes no API calls.
