# Agent Chat Sidebar, Toolbar, and Model State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework the Agent chat navigation hierarchy and ensure a selected text model is immediately reflected in the input control.

**Architecture:** Keep session storage and actions unchanged while replacing the drawer and row presentation in-place. Split the shared toolbar into an Agent-only left action group and right title, while preserving the existing title/a11y arrangement on other tabs. Align model selection acceptance with the same provider-readiness rule used by the selector list, persist the selection, and immediately render the selected model.

**Tech Stack:** Android Views/XML, Kotlin, AppCompat `DialogFragment`, Activity Result API, Gradle/AAPT.

---

### Task 1: Make the history drawer a full-height navigation surface

**Files:**
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/session/AgentSessionDrawerDialog.kt`
- Modify: `libs/features/libAgent/src/main/res/layout/drawer_agent_sessions.xml`
- Modify: `libs/features/libAgent/src/main/res/drawable/bg_agent_session_drawer.xml`
- Create: `libs/features/libAgent/src/main/res/drawable/bg_agent_drawer_icon_button.xml`
- Create: `libs/features/libAgent/src/main/res/drawable/bg_agent_new_session_row.xml`
- Create: `libs/features/libAgent/src/main/res/drawable/ic_agent_add.xml`

- [ ] **Step 1: Remove floating-card visual cues**

Use a flat edge-to-edge drawer surface with a subtle right divider, compact header, quiet close action, and a navigation-row treatment for “New chat”.

- [ ] **Step 2: Apply system bar insets to drawer content**

Use `WindowCompat.setDecorFitsSystemWindows(window, false)` and apply status/navigation bar insets as root padding so the background reaches both screen edges while content stays readable.

- [ ] **Step 3: Verify resource linking**

Run: `./gradlew :publish:zpublishScript:processDebugResources`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Put history and new-chat actions left, Agent title right

**Files:**
- Modify: `appScript/src/main/res/layout/main_toolbar.xml`
- Modify: `appScript/src/main/java/com/hive/ActivityTab.kt`

- [ ] **Step 1: Add a dedicated Agent title**

Keep the normal tab title on the left for non-Agent tabs. When the Agent tab is active, hide that title, show history and add actions at the left edge, and show a separate Agent title at the right edge.

- [ ] **Step 2: Verify visibility state transitions**

Update both title text and visibility inside the existing toolbar refresh path so tab switching cannot leave stale toolbar elements visible.

### Task 3: Simplify the session row

**Files:**
- Modify: `libs/features/libAgent/src/main/res/layout/item_agent_session.xml`
- Modify: `libs/features/libAgent/src/main/res/drawable/selector_agent_session_row.xml`
- Modify: `libs/features/libAgent/src/main/res/drawable/bg_agent_session_current_badge.xml`

- [ ] **Step 1: Replace bordered cards with compact navigation rows**

Use a transparent default row, soft pressed/selected states, one-line title and preview, quiet timestamp, explicit “Current” text, and a 44dp overflow target.

- [ ] **Step 2: Preserve behavior**

Keep row click for switching and overflow actions for convert/delete; do not change session storage or callbacks.

### Task 4: Synchronize the selected model with the input control

**Files:**
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/AgentChatFragment.kt`
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/ai/AIServiceManager.kt`

- [ ] **Step 1: Use one provider readiness rule**

Accept a selected model when its provider reports `isProviderReady()`, matching the model list filter, instead of rechecking only API-key validity.

- [ ] **Step 2: Persist selection synchronously**

Write selected model preferences with `putStringImmediately` so the subsequent UI read cannot observe stale state.

- [ ] **Step 3: Render selected model immediately**

After a successful result, pass the selected model directly to the model-selector renderer; retain `onResume` refresh as a fallback.

### Task 5: Regression verification

**Files:**
- Test only; no new files.

- [ ] **Step 1: Run Kotlin compilation**

Run: `./gradlew :appScript:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run final app resource linking**

Run: `./gradlew :publish:zpublishScript:processDebugResources`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Check patch integrity**

Run: `git diff --check`

Expected: no output.
