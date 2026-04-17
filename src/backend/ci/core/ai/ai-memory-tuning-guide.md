# AI Memory 上下文压缩调优指南

> 基于 AgentScope Java SDK v1.0.11 + Kimi K2.5（262K context window）

## 1. 架构概览

```
用户消息 → Supervisor (ReActAgent)
               ├── AutoContextMemory  ← 管理上下文，触发压缩
               ├── AutoContextHook    ← 每次 Reasoning 前调用 compressIfNeeded()
               └── 子智能体 (短生命周期，不压缩)
```

- **Supervisor** 配置 `AutoContextHook`，在每次 Reasoning 前自动检查是否需要压缩。
- **子智能体** 每次由 Supervisor 调用时新建，生命周期短，不注册 `AutoContextHook`，不触发压缩。

## 2. 单位说明

SDK 内部有两种度量单位，容易混淆：

| 度量 | 说明 | 换算 |
|------|------|------|
| **token 数** | LLM 计费单位 | SDK 估算：`字符数 / 2.5`；Kimi 实际：`字符数 / 3.5` |
| **字符数** | `String.length()` | 1 汉字 = 1 字符，1 英文字母 = 1 字符 |

各参数单位对照：

| 单位 | 参数 |
|------|------|
| token 数 | `maxToken`、`minCompressionTokenThreshold` |
| 字符数 | `largePayloadThreshold`、`offloadSinglePreview` |
| 消息条数 | `msgThreshold`、`lastKeep`、`minConsecutiveToolMessages` |
| 比例 (0.0-1.0) | `tokenRatio`、`currentRoundCompressionRatio` |

换算参考（基于 SDK 的 `chars / 2.5`）：

| token 数 | ≈ 字符数 | ≈ 中文字数 |
|----------|----------|-----------|
| 1K | 2,500 | 2,500 |
| 10K | 25,000 | 2.5 万 |
| 100K | 250,000 | 25 万 |

## 3. 压缩触发条件

满足**任一**即触发 `compressIfNeeded()`：

| 条件 | 公式 | 当前默认值 |
|------|------|-----------|
| 消息数兜底 | `msgCount ≥ msgThreshold` | 200 条（≈ 20-50 轮对话） |
| token 占比 | `tokenCount ≥ maxToken × tokenRatio` | 225,280 × 0.7 ≈ **157K tokens** |

额外保护：即使满足以上条件，若 `tokenCount < minCompressionTokenThreshold`（4096），也不执行压缩。

## 4. 六大压缩策略

触发后按优先级从高到低**依次检查**，命中任一即停止（不会继续检查后续策略）。

### Strategy 1：工具调用压缩

| 属性 | 值 |
|------|---|
| 事件类型 | `TOOL_INVOCATION_COMPRESS` |
| 压缩目标 | **历史轮**的连续工具调用消息 |
| 是否调 LLM | 是（同步，阻塞） |
| 触发条件 | 连续工具消息 ≥ `minConsecutiveToolMessages`(8) 且该组 tokens ≥ `minCompressionTokenThreshold`(4096) |
| 受 lastKeep 保护 | 是 |

适用场景：Supervisor 一次调了多个子智能体，产生大量 tool_call / tool_result 消息。

### Strategy 2：大消息 Offload（有 lastKeep 保护）

| 属性 | 值 |
|------|---|
| 事件类型 | `LARGE_MESSAGE_OFFLOAD_WITH_PROTECTION` |
| 压缩目标 | **历史轮**中超大单条消息 |
| 是否调 LLM | **否，零成本** |
| 触发条件 | 单条消息 `String.length() ≥ largePayloadThreshold`(100,000)，且不在最近 `lastKeep`(20) 条内 |
| 处理方式 | 替换为前 `offloadSinglePreview`(10,000) 字符的预览 + UUID 引用 |

最理想的策略 — 不调 LLM，零延迟。Agent 可通过 `ContextOffloadTool` 按 UUID 重新加载完整内容。

### Strategy 3：大消息 Offload（无 lastKeep 保护）

| 属性 | 值 |
|------|---|
| 事件类型 | `LARGE_MESSAGE_OFFLOAD` |
| 压缩目标 | **历史轮**中超大单条消息 |
| 是否调 LLM | **否，零成本** |
| 触发条件 | 同 Strategy 2，但忽略 `lastKeep` 保护 |

Strategy 2 的升级版，在 Strategy 2 找不到可 offload 的消息（都在 lastKeep 范围内）时使用。

### Strategy 4：历史轮对话摘要

| 属性 | 值 |
|------|---|
| 事件类型 | `PREVIOUS_ROUND_CONVERSATION_SUMMARY` |
| 压缩目标 | **历史轮**的 user-assistant 对话对 |
| 是否调 LLM | 是（同步，阻塞） |
| 触发条件 | 单轮对话 tokens ≥ `minCompressionTokenThreshold`(4096) |
| 压缩比例 | `currentRoundCompressionRatio`(0.4) |

从最新的历史轮向前逐轮检查，找到第一个超过阈值的轮次进行摘要。

### Strategy 5：当前轮大消息摘要

| 属性 | 值 |
|------|---|
| 事件类型 | `CURRENT_ROUND_LARGE_MESSAGE_SUMMARY` |
| 压缩目标 | **当前轮**中的大消息 |
| 是否调 LLM | 是（同步，阻塞） |
| 触发条件 | 当前轮存在超大消息 |

### Strategy 6：当前轮消息压缩（兜底）

| 属性 | 值 |
|------|---|
| 事件类型 | `CURRENT_ROUND_MESSAGE_COMPRESS` |
| 压缩目标 | **当前轮**全部消息 |
| 是否调 LLM | 是（同步，阻塞） |
| 触发条件 | 前面 5 个策略全部 SKIP 时兜底执行 |
| 压缩比例 | `currentRoundCompressionRatio`(0.4) |

> **⚠️ 已知问题（Bug #1026，目标 v1.0.12 修复）：**
>
> Strategy 6 会破坏 ReAct 结构，导致：
> - 压缩后内容比原文更大（实测膨胀 2-6 倍）
> - 压缩后内容为空（`actual: 0 chars`）
> - 阻塞 60-99 秒
>
> **规避方法**：提高 `msgThreshold`，让压缩尽量由 token 阈值触发，此时 Strategy 1/4 有足够大的单轮数据量可以真正压缩，不会落到 Strategy 6。

## 5. 配置参数详解

```yaml
ai:
  memory:
    # ─── 模型硬约束 ──────────────────────────────────────
    # Kimi K2.5: 262,144 tokens - 36K 输出预留 ≈ 225K
    max-token: 225280

    # ─── 压缩触发条件（满足任一即触发）───────────────────
    # 消息数兜底，约 20-50 轮复杂交互
    msg-threshold: 200
    # token 触发：225280 × 0.7 ≈ 158K tokens
    token-ratio: 0.7
    # 低于 4K tokens 不值得压缩
    min-compression-token-threshold: 4096

    # ─── 压缩策略 ─────────────────────────────────────────
    # 保留最近 20 条消息不压缩
    last-keep: 20
    # 压缩后保留原始 40%
    current-round-compression-ratio: 0.4

    # ─── 大消息 Offload ───────────────────────────────────
    # 单条 > 100K 字符才 offload
    large-payload-threshold: 100000
    # offload 后保留 10000 字符预览
    offload-single-preview: 10000
    # 连续 ≥ 8 条工具消息才触发 Strategy 1
    min-consecutive-tool-messages: 8
```

## 6. 调优经验

### 6.1 "卡住"问题排查

**现象**：前端显示"调用中"长时间无进展。

**根因**：压缩（尤其 Strategy 6）同步阻塞 Agent，期间无事件流出。若阻塞时间超过 SSE 超时（`STREAM_TIMEOUT_MINUTES`），前端断连，Agent 变成孤儿继续执行。

**排查步骤**：

1. 搜索日志 `Compression triggered`，确认压缩是否被触发
2. 搜索 `Strategy 6: APPLIED`，确认是否落到了兜底策略
3. 对比压缩前后时间戳，计算阻塞时长
4. 搜索 `[AguiChat] Timeout`，确认是否触发了 SSE 超时

**解决**：
- 提高 `msgThreshold`，避免消息数过早触发压缩
- 提高 `STREAM_TIMEOUT_MINUTES`（当前 15 分钟）

### 6.2 `msgThreshold` 过低的症状

```
Compression triggered - msgCount: 114/100, tokenCount: 36382/157696
```

消息数已到阈值但 token 才占 23%，说明 `msgThreshold` 过低。此时每轮对话 tokens 不到 4096，Strategy 1-5 全部 skip，最终落到 Strategy 6 产生垃圾压缩。

### 6.3 "越压越大"的问题

Strategy 6 的 LLM 摘要输出经常比输入更大：

| 输入 tokens | 输出 tokens | 膨胀倍数 |
|------------|------------|---------|
| 606 | 3,408 | 5.6x |
| 861 | 3,311 | 3.8x |
| 1,391 | 6,263 | 4.5x |

这是 Bug #1026，无法通过参数调优解决，只能通过提高 `msgThreshold` 规避。

### 6.4 压缩日志无输出

如果日志中搜不到任何 `Compression triggered`：

1. 确认 `AutoContextHook` 已注册（在 `SubAgentFactory.buildHooks` 中 `includeAutoContext = true`）
2. 确认日志级别 `logging.level.io.agentscope: INFO`
3. 注意：`compressIfNeeded()` 在阈值未达到时**静默返回 false**，不输出任何日志

### 6.5 子智能体是否需要压缩

**不需要。** 子智能体每次由 Supervisor 调用时新建，生命周期短（通常 1-2 轮 Reasoning），上下文不会积累到需要压缩的程度。给子智能体加 `AutoContextHook` 反而会：
- 注册不必要的 `ContextOffloadTool`
- 增加每次 Reasoning 前的检查开销

## 7. 已知 SDK Bug

| Bug | 版本 | 影响 |
|-----|------|------|
| **#989** | v1.0.11 已修复 | Strategy 1 `summaryToolsMessages()` 跳过压缩但标记为成功，阻止后续策略执行 |
| **#1042** | v1.0.11 已修复 | 大消息 offload 时 `tool_calls` / `tool_result` 配对断裂 |
| **#1026** | v1.0.12 目标 | Strategy 6 破坏 ReAct 结构，导致膨胀或内容为空 |
| **#856** | 开放 | `compressIfNeeded()` 任一策略成功后立即返回，不检查是否仍超标 |

## 8. 监控指标

日志中的 `[Timing] Reasoning START` 输出当前上下文指标（已转换为 token 单位）：

```
totalTokens=12400, msgsTokens=5200, toolsTokens=7200, tools=26, msgs=2, sysPromptTokens=2918
```

- `msgsTokens` 是压缩判断的依据（对应 `maxToken × tokenRatio` 阈值）
- `totalTokens` 包含 tools schema，不参与压缩判断
- 当 `msgsTokens` 接近 157K 时会触发压缩

压缩事件记录格式：

```
type=TOOL_INVOCATION_COMPRESS,in=3489,out=3013,msgs=31; type=PREVIOUS_ROUND_CONVERSATION_SUMMARY,in=6186,out=2872,msgs=5
```

- `in` / `out`：LLM 压缩的输入/输出 token 数
- `msgs`：被压缩的消息数
- `out > in` 表示"膨胀"，通常发生在 Strategy 6
