# KletsAI: personal AI Dutch tutor

A personal app for going from A1 to A2 Dutch (goal: June 2027), used mostly on a phone on the train.
MVP scope is **only**: SM-2 flashcards, a bilingual AI chat tutor with role-play scenarios, and mistake memory
(corrected mistakes become cards). Don't add features beyond this without asking.

Full design (tables, endpoints, decisions): @docs/DESIGN.md

## Stack
- **Backend** (`/backend`): Spring Boot 4.1, Java 21 (`<java.version>21</java.version>`), Maven wrapper (`./mvnw`). Note: Boot 4 test annotations moved packages (e.g. `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`)
- **AI**: Spring AI 2.0 (Anthropic starter, built on the official `anthropic-java` SDK), used only inside `SpringAiTutorClient` behind the `TutorClient` interface. Settings in `kletsai.ai.*` (model `claude-opus-5-5`, effort `low`, adaptive thinking, native structured output from `prompts/tutor-reply.schema.json`, conversation-history caching). Always set the model: Spring AI's default is an old Haiku. Newer Claude models reject `temperature`/`top_p` and disabled thinking, so don't set those
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
- **Secrets / config**: `ANTHROPIC_API_KEY`, `DATABASE_URL` (JDBC form), `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `ALLOWED_ORIGINS`, `PORT` come only from the environment or `.env`, with local-dev defaults in `application.yml`. Never commit `.env` and never hard-code keys.
- **Frontend API base**: every HTTP call uses `API` from `core/api-url.ts` (built from `environment.apiUrl`). Never hard-code `/api` or a host.
- **Users**: every user-owned table has `user_id`. Get the user from `CurrentUser` (returns the seeded single user for now). Never hard-code the UUID anywhere else.
- **Migrations**: never edit a migration that has already run. Add `V{n}__description.sql`. JPA uses `ddl-auto: validate`.
- **Backend**: packages by feature (`flashcard`, `review`, `chat`, `mistake`, `ai`, `common`). Use records for DTOs and constructor injection. Keep SM-2 a pure class with no Spring or DB code. Return errors as RFC 7807 `ProblemDetail`.
- **AI**: only `TutorClient` talks to Claude. The tutor's reply is structured JSON (`reply[{nl,en}]` + `corrections[]`), never free text that has to be parsed. Prompts live in `resources/prompts/`.
- **Tutor behaviour**: simple A1 Dutch, short sentences, every Dutch line followed by its English translation, gentle corrections, grammar explained by comparing with English.
- **Frontend**: mobile-first, designed at 375px wide first. Colours are CSS variables in `src/styles.scss` (light + dark). Keep logic in plain TS classes (e.g. `ReviewSession`) so it can be tested without TestBed. Use `core/uuid.ts`, not `crypto.randomUUID()`, which doesn't exist over plain http on the phone. One exercise per screen. Tap targets at least 48px. Feature folders under `src/app/features`. All HTTP goes through `core/` services.
- **Offline**: reviews go into an IndexedDB queue with a client-generated UUID and are sent to `POST /api/reviews/sync`. The server is the only place SM-2 runs.

## Deployment (prepared, not live)
- **Backend** → Render web service from `backend/Dockerfile` (multi-stage, JRE 21 alpine, `JAVA_OPTS=-Xmx300m`, non-root). Health check: `/actuator/health` (the only exposed actuator endpoint). Flyway migrates on startup.
- **DB** → Neon PostgreSQL. Use the direct (non-pooled) host in JDBC form: `jdbc:postgresql://<host>/<db>?sslmode=require`.
- **Frontend** → Render static site: build `npm ci && npm run build -- --configuration production`, publish `dist/frontend/browser`, rewrite `/*` → `/index.html`. Set the backend URL in `src/environments/environment.prod.ts`.
- The Docker build skips tests (no Docker inside it): run `./mvnw test` before pushing.

## Working agreement
The project is built in steps: (1) backend skeleton + DB → (2) flashcards → (3) AI chat → (4) mistake memory → (5) PWA/offline.
**Stop after each step** with what was built and how to test it, and wait for the user before continuing.
