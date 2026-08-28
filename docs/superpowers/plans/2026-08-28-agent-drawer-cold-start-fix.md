# Agent Drawer And Cold Start State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Agent history drawer truly edge-to-edge, align it with the app design system, and restore the configured AI model correctly during cold start.

**Architecture:** Keep the existing `DialogFragment` drawer and session list, but remove the default dialog frame and explicitly configure transparent system bars so its surface occupies the whole window. Reuse the app's design tokens for drawer and item styling. Treat Agent service initialization as an asynchronous state boundary: the chat UI must wait for the service-ready event before deciding that model configuration is missing.

**Tech Stack:** Android Views/XML, Kotlin, DialogFragment, WindowInsetsCompat, EventBus, Gradle Android resource/Kotlin/lint tasks.

---

### Task 1: Make the drawer window edge-to-edge

**Files:**
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/session/AgentSessionDrawerDialog.kt`
- Modify: `libs/features/libAgent/src/main/res/values/styles.xml`

- [x] **Step 1: Remove the default floating-dialog frame**

Call `setStyle(STYLE_NO_FRAME, R.style.AgentSessionDrawerDialog)` from `onCreate`, and define a no-title transparent dialog theme.

- [x] **Step 2: Configure system bars explicitly**

Set transparent status/navigation bar colors, clear translucent flags, enable system-bar background drawing, and retain `setDecorFitsSystemWindows(false)`.

- [x] **Step 3: Keep content safe while the surface stays full-height**

Apply status/navigation inset values only as internal root padding; do not inset or shorten the dialog window itself.

### Task 2: Align drawer and item styling with the app

**Files:**
- Modify: `libs/features/libAgent/src/main/res/layout/drawer_agent_sessions.xml`
- Modify: `libs/features/libAgent/src/main/res/layout/item_agent_session.xml`
- Modify: `libs/features/libAgent/src/main/res/drawable/bg_agent_session_drawer.xml`
- Modify: `libs/features/libAgent/src/main/res/drawable/bg_agent_new_session_row.xml`
- Modify: `libs/features/libAgent/src/main/res/drawable/selector_agent_session_row.xml`

- [x] **Step 1: Reuse app surfaces and spacing**

Use `design_bg_overlay`, `design_bg_card`, `design_border_subtle`, the shared spacing scale, 56dp toolbar rhythm, and 16dp app card radius.

- [x] **Step 2: Refine the new-session action**

Render it as a compact app-style card row with a clear plus icon and native pressed feedback.

- [x] **Step 3: Refine session items**

Use a compact card row, keep title/preview/time hierarchy legible, keep the current badge explicit, and preserve 44dp touch targets for overflow actions.

### Task 3: Fix cold-start model restoration

**Files:**
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/AgentChatFragment.kt`
- Modify: `appScript/src/main/java/com/hive/ui/agent/AgentMainFragment.kt`

- [x] **Step 1: Stop treating an uninitialized Agent service as missing configuration**

Make environment prompting return early while the AI service manager or provider registry is unavailable.

- [x] **Step 2: Refresh model UI at the service-ready boundary**

Expose a chat-fragment refresh method and invoke it from `AgentMainFragment` for both `AGENT_SERVICE_START` and `AGENT_SERVICE_MCP_REGISTERED`.

- [x] **Step 3: Re-evaluate the prompt only after initialization**

Refresh the selector text first, then run the environment check so a persisted model is displayed and not incorrectly cleared or reported as missing.

### Task 4: Verify and ship

**Files:**
- Verify all files above.

- [x] **Step 1: Run resource linking**

Run `./gradlew :publish:zpublishScript:processDebugResources` and expect `BUILD SUCCESSFUL`.

- [x] **Step 2: Run Kotlin compilation**

Run `./gradlew :appScript:compileDebugKotlin` and expect `BUILD SUCCESSFUL`.

- [x] **Step 3: Run focused lint and diff checks**

Run `./gradlew :libs:features:libAgent:lintDebug` and `git diff --check`; expect both to pass.

- [ ] **Step 4: Commit and push**

Review the intended diff, create one concise Chinese commit, and push the current branch to its configured upstream.
