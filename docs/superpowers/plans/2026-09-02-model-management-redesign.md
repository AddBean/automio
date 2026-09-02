# Model Management Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild model management so providers discover models and capabilities automatically, custom models can be safely deleted, and connection/model state is understandable at a glance.

**Architecture:** Add a provider-independent capability detector and let the custom OpenAI-compatible provider discover `/models`. Keep remote/built-in models immutable while custom models expose destructive actions. Render provider and model state with explicit text, source badges, model IDs, and lifecycle-safe refresh behavior.

**Tech Stack:** Android Views/XML, Kotlin coroutines, `HttpURLConnection`, Gson, SharedPreferences, JUnit.

---

### Task 1: Automatic model capability detection

**Files:**
- Create: `libs/features/libAgent/src/main/java/com/hive/agent/ai/ModelCapabilityDetector.kt`
- Create: `libs/features/libAgent/src/test/java/com/hive/agent/ai/ModelCapabilityDetectorTest.kt`

- [ ] Write tests that cover vision model names, tool-disabled embedding/reranker models, known context-window families, and generic chat fallbacks.
- [ ] Run `./gradlew :libs:features:libAgent:testDebugUnitTest --tests '*ModelCapabilityDetectorTest'` and verify the tests fail because the detector does not exist.
- [ ] Implement `ModelCapabilityDetector.detect(modelId)` returning `ModelCapabilities`, using conservative name/catalog rules and `ModelType.CHAT`.
- [ ] Run the focused unit test and verify it passes.

### Task 2: Discover OpenAI-compatible models and repair persisted state

**Files:**
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/ai/providers/CustomOpenAIProvider.kt`
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/ai/AIServiceManager.kt`
- Modify: `libs/features/libAgent/src/test/java/com/hive/agent/ai/providers/OpenAiUrlHelperTest.kt`

- [ ] Add coverage confirming root and versioned Base URLs resolve to the correct `/models` endpoint.
- [ ] Implement authenticated model-list discovery in `CustomOpenAIProvider`, parse OpenAI-compatible `data[].id`, and map each ID through `ModelCapabilityDetector`.
- [ ] Fall back to persisted custom models when the remote service is unavailable; do not invent a default model.
- [ ] Make `isProviderModelEnabled` read persisted state while treating an absent preference as enabled for migration compatibility.
- [ ] When removing a custom model, remove its enabled-state entry and clear matching TEXT/IMAGE defaults immediately.
- [ ] Persist Provider enable/disable state across process restarts.

### Task 3: Safe model actions and simplified add flow

**Files:**
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/provider/AIServiceManagerFragment.kt`
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/provider/AIProviderItemView.kt`
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/provider/AIModelItemView.kt`
- Modify: `libs/features/libAgent/src/main/res/layout/ai_dialog_input.xml`

- [ ] Remove capability switches from the normal add-model flow and generate capabilities automatically from the entered model ID.
- [ ] Add a custom-model overflow/delete action; built-in and remotely discovered models must not expose deletion.
- [ ] Show an `AlertDialog` containing the exact model display name before deletion.
- [ ] Route deletion through `removeProviderCustomModel`, refresh the list, refresh selected-model summaries, and show success feedback.
- [ ] Bind expanded/collapsed state from data instead of relying on recycled view-local selection state.

### Task 4: Rebuild provider and model presentation

**Files:**
- Modify: `libs/features/libAgent/src/main/res/layout/agent_ai_provider_manager.xml`
- Modify: `libs/features/libAgent/src/main/res/layout/ai_provider_item_new.xml`
- Modify: `libs/features/libAgent/src/main/res/layout/ai_model_item.xml`
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/provider/AIProviderItemView.kt`
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/provider/AIModelItemView.kt`
- Modify: `libs/i8n/src/main/res/values/ft-lib-agent.xml`
- Modify: `libs/i8n/src/main/res/values-zh/ft-lib-agent.xml`

- [ ] Rename the screen to “Models & Services/模型与服务” and label the defaults section and provider section clearly.
- [ ] Give provider cards explicit connection state, model count, larger action targets, and a single expand affordance.
- [ ] Show model display name, stable model ID, tool/vision/context badges, source type, and a textual availability state.
- [ ] Ensure long IDs truncate cleanly and every icon-only action has a content description.
- [ ] Replace nested manual model containers with a layout that remains readable for large synchronized catalogs, keeping the initial scope compatible with the existing list framework.

### Task 5: Refresh behavior and verification

**Files:**
- Modify: `libs/features/libAgent/src/main/java/com/hive/agent/views/provider/AIServiceManagerFragment.kt`
- Modify: dynamic provider cache implementations as required.

- [ ] Load provider model lists concurrently under `supervisorScope`, preserving partial successes.
- [ ] Make global synchronization invalidate all dynamic-provider caches, not only Ollama.
- [ ] Keep the current content visible while refreshing and prevent duplicate refresh jobs.
- [ ] Run focused unit tests, `./gradlew :libs:features:libAgent:compileDebugKotlin`, and resource lint/assembly available for this repository.
- [ ] Review `git diff`, confirm only intended files changed, create a Chinese commit, merge into `p/jiadou/dev`, and push `origin/p/jiadou/dev`.

