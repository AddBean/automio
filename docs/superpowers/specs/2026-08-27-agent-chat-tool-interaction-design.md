# Agent 对话列表工具交互改版

日期：2026-08-27  
状态：已确认  
范围：`libs/features/libAgent` 聊天列表中的工具消息展示、思考态 loading、自动滚底

## 背景

当前工具消息在列表内可展开，默认就能看到方法名/参数/结果，占位大、干扰阅读。思考态下空的 `WAITING` assistant 气泡被隐藏，底部看不到 loading；同时 `WAITING` 会给末条换背景/挂 statusIndicator，形成「高亮最后一条」的观感。自动滚底逻辑已有雏形，需在新增尾占位后继续可靠工作。

## 目标

1. 列表内工具 item 默认不展示 tools 详情；点击后弹层查看。
2. 工具 item 仅展示工具名 + 调用状态；背景宽度包裹内容；整体略缩小。
3. 思考时列表底部有 loading；不必高亮最后一个消息 item。
4. 用户已在底部时，有更新则自动滚到底。

## 方案

采用：**紧凑 chip + BottomSheet 详情 + 列表尾 loading**（与会话列表 BottomSheet、「压缩记忆中」尾占位一致）。

---

## 1. 工具 chip（列表内）

### UI

- 单行：工具显示名 + 右侧状态图标（执行中 / 成功 / 失败）。
- 移除展开箭头与内联详情区（方法/参数/结果不再出现在列表）。
- 容器 `layout_width=wrap_content`，背景宽度包内容，左对齐。
- 尺寸略缩小：padding 约 `8×6dp`，标题 `11sp`，状态图标 `14dp`，外层垂直 padding `2dp`。
- 背景继续用现有 `tool_message_container_bg` 风格（可按新尺寸微调圆角，不新开视觉体系）。

### 交互

- 单击 → 打开工具详情 BottomSheet。
- 长按复制：保留（复制 content / toolCallResult）。

### 状态映射

| 条件 | 图标 |
|------|------|
| 无 `toolCallResult` | running |
| 有结果且 success | check |
| 有结果且失败 | close |

### 主要改动文件

- `chat_message_tool_item.xml`
- `ChatMessageItemToolView.kt`（去掉 expand 状态机；点击改打开 sheet）

---

## 2. 工具详情 BottomSheet

### 形态

- `BottomSheetDialogFragment`，视觉对齐 `AgentSessionListBottomSheet`：
  - 顶圆角背景 `#0E1014`（`bg_agent_session_sheet`）
  - 顶部 drag handle
  - 透明窗口 / sheet 背景
- 内容区可滚动（参数与结果可能很长）。

### 内容结构（上→下）

1. Handle  
2. 标题行：工具显示名 + 状态图标  
3. 方法名（label + value）  
4. 参数（label + value，小字）  
5. 执行结果（label + 结果文本；有图则显示图片）  
6. 无结果时结果区显示执行中占位文案  

字段语义与现有内联详情一致（slate 次要色、结果区可读），仅从列表挪到 sheet。

### 打开方式

- `ChatMessageItemToolView` 点击 → 通过宿主 Fragment 的 `childFragmentManager` show。
- 传入当前 `ChatMessage` 快照；打开后不实时跟随刷新；关闭再点可看最新。

### 主要新增

- `bottom_sheet_agent_tool_detail.xml`
- `AgentToolDetailBottomSheet.kt`（放在 `views/session` 同级的 `views/chat` 或 `views` 包，与会话 sheet 同模式）
- i18n：优先复用 `agent_tool_*` / `agent_tool_executing`；缺则补中英字符串

---

## 3. 思考 loading + 取消末条高亮

### 列表尾占位

- 新增 `TYPE_THINKING_LOADING`（工厂与 `AgentChatView.buildDisplayData`）。
- UI：居中 `AVLoadingIndicatorView`（`BallBeatIndicator`，accent 色），风格对齐现有 assistant 上的指示器与 compressing 尾占位。
- 出现条件（在压缩占位未启用时）：取 `messages` 中最后一条非 system 消息，若其为 `ASSISTANT` 且 `status == WAITING`，则在展示列表末尾追加 thinking loading。  
  - 该条若为空（无 content / reasoning / image）则不进入展示列表，但仍满足上述条件 → 只显示尾 loading。  
  - 该条若已有内容则正常展示气泡，同时保留尾 loading，直到 status 离开 `WAITING`。

### 气泡规则

- 空 WAITING assistant：**不渲染**成气泡。
- 已有内容的 assistant：统一使用 finish 背景；**不再**因 `WAITING` 换背景；**不再**显示 item 上的 `statusIndicator`。
- 「不必高亮最后一个 item」= 去掉 WAITING 专用气泡样式与 item 内 loading，改由列表尾统一表达。

### 与压缩记忆占位

- 压缩记忆中时只展示压缩占位，不叠思考 loading（互斥，压缩优先）。

### 主要改动文件

- `ChatMessageViewFactory.kt`
- `AgentChatView.kt`（`buildDisplayData` / fingerprint）
- `ChatMessageItemAssistantView.kt`（去掉 WAITING 背景与 statusIndicator 绑定）
- `chat_message_thinking_item.xml`
- `ChatMessageItemThinkingView.kt`

---

## 4. 自动滚到底

### 规则

- 更新前用 `!listRecyclerView.canScrollVertically(1)` 判断是否在底部。
- **仅在已在底部时**，在消息追加、流式尾部变化、尾 loading 出现/消失后，滚到 `lastIndex`。
- 用户上滑看历史时不抢滚动。
- 不强制始终吸底；流式场景继续用无动画 `scrollToPosition`（高度刚变时用 `post`）。

### 实现

- 继续 `AgentChatView.updateChatList()` 现有分支（纯追加 / 同大小尾部变化 / 全量刷新）。
- 滚动目标一律包含尾占位后的 `lastIndex`。
- fingerprint 需覆盖 thinking loading 占位，避免无意义全量刷新或漏滚。

---

## 非目标

- 不改工具调用协议 / Agent 主循环逻辑。
- 不改会话列表 BottomSheet。
- 不做工具详情打开后的实时流式刷新。
- 不引入新的全局设计语言（颜色/字体体系沿用现有）。

## 测试要点

1. 工具 chip 只显示名+状态，宽度 wrap；点击打开 sheet，内容含方法/参数/结果/图。
2. 长按工具仍可复制。
3. 发送后思考中：底部出现 BallBeat；无空气泡高亮；有流式正文后气泡正常出现，尾 loading 在仍 WAITING 时保留或按规则消失时机正确（WAITING→有内容仍 WAITING 时：有气泡 + 尾 loading；finalize 后尾 loading 消失）。
4. 压缩记忆中时只见压缩占位，不见思考 loading。
5. 在底部时流式输出/工具追加会跟滚；上滑离开底部后不再跟滚。

## 风险与注意

- BottomSheet 需从 Fragment 打开：由 `AgentChatFragment`（或 `AgentChatView`）注入 `onToolMessageClick` 回调，ItemView 只回调 `ChatMessage`，不自行找 FragmentManager。
- 列表 item 变矮后，底部判定与 scroll 时机更敏感，依赖 `post` 避免 layout 未完成。
- 去掉 assistant WAITING 高亮后，思考反馈完全依赖尾 loading，空闲态误留 WAITING 消息会表现为「一直转圈」——沿用现有 session 恢复时清理假 WAITING 的逻辑即可。
