# Stage A Feature and UX Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete anniversary and wish editing workflows, replace native confirmations, and standardize core loading/error/empty interactions without changing the existing visual identity.

**Architecture:** Add small shared UI primitives and pure form-normalization helpers, then integrate them incrementally into existing pages. Use a singleton confirmation service mounted once in `App.vue`; keep API compatibility and existing page routing.

**Tech Stack:** Vue 3, Pinia, Vue Router, Vite, Vitest, Vue Test Utils.

---

### Task 1: Establish the minimal frontend test harness

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.js`
- Create: `frontend/src/test/setup.js`

- [ ] Add Vitest, jsdom and Vue Test Utils dev dependencies plus a `test` script.
- [ ] Configure Vitest with jsdom, Vue plugin and `@` alias.
- [ ] Run `npm test -- --run`; expected result: runner starts successfully with no test files.

### Task 2: Build and test the confirmation service

**Files:**
- Create: `frontend/src/composables/confirmService.js`
- Create: `frontend/src/composables/confirmService.test.js`
- Create: `frontend/src/components/ConfirmDialog.vue`
- Create: `frontend/src/components/ConfirmDialog.test.js`
- Modify: `frontend/src/App.vue`

- [ ] Write failing tests proving `requestConfirm` resolves true on confirm, false on cancel, and supersedes no active request.
- [ ] Run the focused tests and verify failure because the service/component is missing.
- [ ] Implement reactive singleton state, Promise resolution and accessible dialog buttons.
- [ ] Mount one `ConfirmDialog` in `App.vue`.
- [ ] Run focused tests; expected result: all confirmation tests pass.

### Task 3: Build reusable page feedback primitives

**Files:**
- Create: `frontend/src/components/BaseModal.vue`
- Create: `frontend/src/components/LoadingState.vue`
- Create: `frontend/src/components/EmptyState.vue`
- Create: `frontend/src/components/ErrorState.vue`
- Create: `frontend/src/components/BaseModal.test.js`

- [ ] Write failing component tests for ESC close, overlay close, busy-state protection and retry emission.
- [ ] Implement focused components using existing CSS variables.
- [ ] Run focused tests and verify they pass.

### Task 4: Complete anniversary CRUD

**Files:**
- Create: `frontend/src/features/anniversary/anniversaryForm.js`
- Create: `frontend/src/features/anniversary/anniversaryForm.test.js`
- Modify: `frontend/src/views/AnniversaryView.vue`

- [ ] Write failing tests for backend/UI type mapping, edit-form hydration and normalized API payload.
- [ ] Implement pure form helpers.
- [ ] Integrate `updateAnniversary` and `deleteAnniversary`, reuse one modal for create/edit, preserve input on failures, and show retryable load errors.
- [ ] Run focused tests and `npm run build`.

### Task 5: Complete wish editing

**Files:**
- Create: `frontend/src/features/wish/wishForm.js`
- Create: `frontend/src/features/wish/wishForm.test.js`
- Modify: `frontend/src/views/WishListView.vue`

- [ ] Write failing tests for create/edit hydration and payload normalization that preserves status.
- [ ] Implement helpers and integrate `updateWish` using the existing create form as an edit form.
- [ ] Replace wish deletion confirmation with `requestConfirm` and update lists locally after successful actions.
- [ ] Run focused tests and `npm run build`.

### Task 6: Remove native confirmations from remaining pages

**Files:**
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/AlbumDetail.vue`
- Modify: `frontend/src/views/SurpriseView.vue`
- Modify: `frontend/src/views/SurpriseDetail.vue`

- [ ] Replace each native `confirm` call with awaited `requestConfirm` while preserving current messages and early-return behavior.
- [ ] Search `frontend/src` for `confirm(`; expected result: zero native calls.
- [ ] Run all frontend tests and production build.

### Task 7: Stage A verification

- [ ] Run `npm test -- --run`.
- [ ] Run `npm run build`.
- [ ] Search for native confirmations and unresolved imports.
- [ ] Inspect anniversary and wish views at desktop and mobile widths if browser runtime is available.
- [ ] Record changed files. The workspace is not a Git repository, so no commit step is possible unless the user later initializes Git.
