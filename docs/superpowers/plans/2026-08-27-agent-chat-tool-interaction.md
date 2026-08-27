# Agent 对话工具交互改版 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (or subagent-driven-development). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 列表工具改为紧凑 chip + BottomSheet 详情；思考态用列表尾 loading；在底部时自动跟滚。

**Architecture:** 展示数据在 `AgentChatView.buildDisplayData` 统一组装（过滤空 WAITING、追加 thinking/compressing 尾占位）。工具 chip 通过 `postEvent` 通知 Fragment 打开 `AgentToolDetailBottomSheet`。Assistant 气泡去掉 WAITING 高亮与 item 内 loading。

**Tech Stack:** Android Kotlin、BottomSheetDialogFragment、ListRecyclerView、AVLoadingIndicatorView

**Spec:** `docs/superpowers/specs/2026-08-27-agent-chat-tool-interaction-design.md`

---

## File map

| File | Role |
|------|------|
| `AgentChatDisplayHelper.kt` (new) | 纯逻辑：从 messages 生成展示列表 |
| `AgentChatDisplayHelperTest.kt` (new) | 单元测试 |
| `chat_message_tool_item.xml` | 紧凑 chip |
| `ChatMessageItemToolView.kt` | chip 绑定 + postEvent |
| `bottom_sheet_agent_tool_detail.xml` | 详情 sheet 布局 |
| `AgentToolDetailBottomSheet.kt` | 详情 sheet |
| `chat_message_thinking_item.xml` | 尾 loading |
| `ChatMessageItemThinkingView.kt` | 尾 loading view |
| `ChatMessageViewFactory.kt` | TYPE_THINKING_LOADING |
| `AgentChatView.kt` | 用 Helper、事件、滚底 |
| `ChatMessageItemAssistantView.kt` | 去掉 WAITING 高亮/indicator |
| `AgentChatFragment.kt` | 打开 tool sheet |

---

### Task 1: Display helper + tests

**Files:**
- Create: `libs/features/libAgent/src/main/java/com/hive/agent/views/chat/AgentChatDisplayHelper.kt`
- Create: `libs/features/libAgent/src/test/java/com/hive/agent/views/chat/AgentChatDisplayHelperTest.kt`

- [ ] **Step 1: 写失败测试**（空 WAITING 不入列表但加 thinking；有内容 WAITING 入列表+thinking；FINISH 无 thinking；compressing 优先；system 过滤）

- [ ] **Step 2: 实现 Helper**（view type 常量与 Factory 对齐：0 user / 1 assistant / 2 system / 3 tool / 4 compressing / 5 thinking）

- [ ] **Step 3: 测试通过并提交**

---

### Task 2: 工具 chip 布局 + View

- [ ] **Step 1: 改 `chat_message_tool_item.xml` 为 wrap_content chip**（去掉 detail 区与 expand 图标）
- [ ] **Step 2: 改 `ChatMessageItemToolView`**：去掉 expand；点击 `postEvent("tool_detail")`；保留状态图标与长按复制；详情字段逻辑挪到 sheet 或保留私有方法给 sheet 复用
- [ ] **Step 3: 提交**

---

### Task 3: Tool detail BottomSheet

- [ ] **Step 1: 新增 layout + `AgentToolDetailBottomSheet`**（对齐 session sheet 视觉；绑定 ChatMessage）
- [ ] **Step 2: `AgentChatView` 转发 item 事件；`AgentChatFragment` show sheet**
- [ ] **Step 3: 提交**

---

### Task 4: Thinking loading + 取消高亮

- [ ] **Step 1: thinking item layout + view + Factory TYPE=5**
- [ ] **Step 2: `AgentChatView` 改用 Helper；fingerprint 覆盖 thinking**
- [ ] **Step 3: AssistantView 统一 finish 背景，隐藏 statusIndicator**
- [ ] **Step 4: 确认滚底目标为 lastIndex（含尾占位）；提交**

---

### Task 5: 编译验证

- [ ] **Step 1: 编译 libAgent 相关模块**
- [ ] **Step 2: 跑 `AgentChatDisplayHelperTest`
