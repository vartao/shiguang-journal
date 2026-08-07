# Stage C Production Security and Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make production configuration fail safely, harden file persistence and likes, and provide health-aware deployment and operational documentation.

**Architecture:** Validate security-sensitive configuration at startup/deploy boundaries, make file persistence compensating-transaction safe, move like-count changes to database atomic updates, and expose actuator health checks consumed by Compose.

**Tech Stack:** Spring Boot Actuator, MyBatis-Plus/custom mapper SQL, MySQL 8, Docker Compose, Nginx, Bash.

---

### Task 1: Fail fast on unsafe production configuration

**Files:** `docker-compose.yml`, `.env.example`, `deploy/deploy.sh`, new `deploy/validate-env.sh`, `README.md`

- [ ] Write shell-level failing checks for missing values, placeholder values, short JWT secrets and missing production CORS.
- [ ] Remove usable default credentials from Compose and deployment scripts.
- [ ] Verify invalid configurations fail and a safe temporary configuration passes `docker compose config`.

### Task 2: Harden file upload persistence

**Files:** `backend/src/main/java/com/qinglv/service/FileService.java`, new/updated tests under `backend/src/test/java/com/qinglv/service/FileServiceTest.java`

- [ ] Write failing tests for blank filename, disallowed MIME, database insert failure cleanup and normalized destination paths.
- [ ] Implement canonical-path containment checks and compensating deletion when persistence fails.
- [ ] Run focused and full backend tests.

### Task 3: Make likes concurrency-safe

**Files:** `backend/src/main/java/com/qinglv/mapper/NoteMapper.java`, `backend/src/main/java/com/qinglv/service/NoteService.java`, `backend/src/test/java/com/qinglv/service/NoteServiceTest.java`

- [ ] Write failing tests requiring atomic increment/decrement mapper calls and non-negative returned counts.
- [ ] Add mapper updates using SQL expressions/conditions and reload authoritative count after toggle.
- [ ] Handle unique-key races as idempotent outcomes.
- [ ] Run focused and full backend tests.

### Task 4: Add health checks

**Files:** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `docker-compose.yml`, `backend/Dockerfile`

- [ ] Add Actuator with only health/info exposed.
- [ ] Add backend container healthcheck and make frontend depend on healthy backend.
- [ ] Validate configuration and build backend image when Docker is available.

### Task 5: Improve deployment readiness and operations

**Files:** `deploy/deploy.sh`, `deploy/nginx.conf`, create `docs/OPERATIONS.md`

- [ ] Replace fixed sleep-only readiness with bounded health polling.
- [ ] Verify SPA fallback, API proxy and uploads alias in nginx config.
- [ ] Document backup, restore, schema migration, rollback and secret rotation commands.

### Task 6: End-to-end verification

- [ ] Run frontend check/test/build and backend full tests.
- [ ] Run `docker compose config` with safe temporary environment values.
- [ ] If Docker daemon is available, build/start the stack, wait for health, then run the acceptance script.
- [ ] If Docker is unavailable, record that runtime E2E is the only unverified item and preserve all static/unit verification evidence.
