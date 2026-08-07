# Stage B Engineering Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add repeatable linting, formatting, frontend tests, backend web-layer tests, and environment verification.

**Architecture:** Extend the minimal Stage A test harness into a project quality gate. Keep rules incremental, extract testable request error helpers where needed, and use Spring MVC slice tests for controller behavior.

**Tech Stack:** ESLint, eslint-plugin-vue, Prettier, Vitest, Vue Test Utils, JUnit 5, Mockito, MockMvc, Maven.

---

### Task 1: Add lint and formatting gates

**Files:** `frontend/package.json`, `frontend/eslint.config.js`, `frontend/.prettierrc.json`, `frontend/.prettierignore`

- [ ] Add ESLint/Prettier dependencies and scripts: `lint`, `lint:fix`, `format`, `format:check`, `check`.
- [ ] Run lint to capture the failing baseline.
- [ ] Fix errors without behavior changes, then verify lint and format checks pass.

### Task 2: Test stores and request error normalization

**Files:** tests beside `frontend/src/stores/*.js`, `frontend/src/api/request.js`, optional `frontend/src/api/requestError.js`

- [ ] Write failing tests for token persistence, logout cleanup, couple normalization, theme initialization, business errors, 401 and timeout messages.
- [ ] Extract only the pure helpers needed for deterministic tests.
- [ ] Run focused tests, then the complete frontend suite.

### Task 3: Add backend controller tests

**Files:** create tests under `backend/src/test/java/com/qinglv/controller/`

- [ ] Write failing MockMvc tests for validation and response envelopes for auth, anniversary, wish and upload endpoints.
- [ ] Add only necessary controller/exception-handler corrections.
- [ ] Run each focused Maven test and then all backend tests with JDK 17.

### Task 4: Add environment and verification scripts

**Files:** create `scripts/check-env.sh`, `scripts/verify.sh`; modify `README.md`

- [ ] Implement Git Bash environment detection for Java 17, Maven, Node and npm.
- [ ] Implement one verification script that runs frontend checks/build and backend tests with the detected JDK 17.
- [ ] Document local commands and the JDK 11 failure symptom.
- [ ] Run both scripts successfully.

### Task 5: Stage B verification

- [ ] Run `npm run check` and `npm test -- --run`.
- [ ] Run `mvn test` under JDK 17.
- [ ] Run `scripts/verify.sh`.
