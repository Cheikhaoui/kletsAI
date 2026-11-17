# KletsAI: personal AI Dutch tutor

A personal app for going from A1 to A2 Dutch (goal: June 2027), used mostly on a phone on the train.
MVP scope is **only**: SM-2 flashcards, a bilingual AI chat tutor with role-play scenarios, and mistake memory
(corrected mistakes become cards). Don't add features beyond this without asking.

Full design (tables, endpoints, decisions): @docs/DESIGN.md

## Stack
- **Backend** (`/backend`): Spring Boot 4.1, Java 21 (`<java.version>21</java.version>`), Maven wrapper (`./mvnw`). Note: Boot 4 test annotations moved packages (e.g. `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`)
- **AI**: Spring AI 2.0 (Anthropic starter), used only inside `TutorClient` in `com.kletsai.ai`. Model id is set in config (default `claude-opus-5-5`). Newer Claude models reject `temperature`/`top_p` and disabled thinking, so don't set those
- **DB**: PostgreSQL 16 in Docker Compose, schema owned by Flyway (`backend/src/main/resources/db/migration`)
- **Frontend** (`/frontend`): Angular 22, standalone components, signals, zoneless, Vitest, `@angular/pwa`. Needs **Node 24 LTS** (Node 23 can't run the tests and its npm 10 install fails)

## Running locally
```bash
cp .env.example .env              # then fill in ANTHROPIC_API_KEY
docker compose up -d              # Postgres on localhost:5432
cd backend && ./mvnw spring-boot:run      # http://localhost:8080 (reads ../.env)
# or, with no local Postgres: ./mvnw spring-boot:test-run  (starts Postgres via Testcontainers)
cd frontend && npm install && npm start   # http://localhost:4200 (proxies /api to :8080)
```
Testing on a phone: `npm start -- --host 0.0.0.0` and open `http://<laptop-ip>:4200`.
The service worker only runs in a production build: `npm run build && npx http-server dist/frontend/browser -p 8081`.

## Tests
```bash
cd backend && ./mvnw test        # unit tests + Testcontainers (Docker must be running); name test classes *Tests
cd frontend && npm test
```
These must have tests: `Sm2Scheduler` (every grade, ease floor 1.3, Again resets, intervals 1 → 6 → ×EF),
review sync (sorted by time, repeated ids ignored), and mistake extraction (parse, validate, deduplicate, card created).
Tests never call the real Claude API. Use a fake `TutorClient`.

## Conventions
- **Secrets**: `ANTHROPIC_API_KEY`, `DB_URL`, `DB_USER`, `DB_PASSWORD` come only from the environment or `.env`. Never commit `.env` and never hard-code keys.
- **Users**: every user-owned table has `user_id`. Get the user from `CurrentUser` (returns the seeded single user for now). Never hard-code the UUID anywhere else.
- **Migrations**: never edit a migration that has already run. Add `V{n}__description.sql`. JPA uses `ddl-auto: validate`.
- **Backend**: packages by feature (`flashcard`, `review`, `chat`, `mistake`, `ai`, `common`). Use records for DTOs and constructor injection. Keep SM-2 a pure class with no Spring or DB code. Return errors as RFC 7807 `ProblemDetail`.
- **AI**: only `TutorClient` talks to Claude. The tutor's reply is structured JSON (`reply[{nl,en}]` + `corrections[]`), never free text that has to be parsed. Prompts live in `resources/prompts/`.
- **Tutor behaviour**: simple A1 Dutch, short sentences, every Dutch line followed by its English translation, gentle corrections, grammar explained by comparing with English.
- **Frontend**: mobile-first, designed at 375px wide first. Colours are CSS variables in `src/styles.scss` (light + dark). Keep logic in plain TS classes (e.g. `ReviewSession`) so it can be tested without TestBed. Use `core/uuid.ts`, not `crypto.randomUUID()`, which doesn't exist over plain http on the phone. One exercise per screen. Tap targets at least 48px. Feature folders under `src/app/features`. All HTTP goes through `core/` services.
- **Offline**: reviews go into an IndexedDB queue with a client-generated UUID and are sent to `POST /api/reviews/sync`. The server is the only place SM-2 runs.

## Working agreement
The project is built in steps: (1) backend skeleton + DB → (2) flashcards → (3) AI chat → (4) mistake memory → (5) PWA/offline.
**Stop after each step** with what was built and how to test it, and wait for the user before continuing.
