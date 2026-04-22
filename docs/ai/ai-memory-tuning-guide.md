# AiMemoryProperties 调优方法论

> 本文档面向 BK-CI 智能体（Agent）开发/运维人员，提供 `AiMemoryProperties` 参数的系统化调优指南。
> 适用于所有使用 `AutoContextMemory` 的智能体（Supervisor、子智能体等）。

---

## 目录

- [1. 架构总览](#1-架构总览)
- [2. 参数速查表](#2-参数速查表)
- [3. 参数分组与依赖关系](#3-参数分组与依赖关系)
- [4. 模型适配指南](#4-模型适配指南)
- [5. 数据驱动调优](#5-数据驱动调优)
  - [5.1 监控基础设施](#51-监控基础设施)
  - [5.2 诊断 SQL](#52-诊断-sql)
  - [5.3 日志快速排查](#53-日志快速排查)
- [6. 调优 SOP（标准作业流程）](#6-调优-sop标准作业流程)
- [7. 典型场景与推荐配置](#7-典型场景与推荐配置)
- [8. 配置参考](#8-配置参考)
- [9. FAQ](#9-faq)

---

## 1. 架构总览

```
用户消息 ──→ AutoContextMemory ──→ LLM 推理
                    │
                    ├─ 检查是否触发压缩 ──→ 组1 参数
                    ├─ 执行压缩策略     ──→ 组2 参数
                    └─ 大消息 Offload   ──→ 组3 参数
```

`AiMemoryProperties`（对应 `@ConfigurationProperties("ai.memory")`）控制 `AutoContextConfig`
的构建参数，决定了 **何时压缩、怎么压缩、大消息怎么处理** 三个核心策略。

**数据流**：

```
AiMemoryProperties (YAML)
       │
       ▼
AiMemoryConfig (@Bean)
       │
       ▼
AutoContextConfig (SDK 配置对象)
       │
       ▼
AutoContextMemory (运行时记忆管理)
       │
       ├── 消息存储 → 检查触发条件 → 执行压缩 → 反馈 CompressionEvent
       └── AgentStageTimingHook (Hook 监听) → T_AI_AGENT_STAGE 表 + 日志
```

---

## 2. 参数速查表

| 参数 | 类型 | 默认值 | YAML key | 说明 |
|---|---|---|---|---|
| `msgThreshold` | Int | 20 | `ai.memory.msg-threshold` | 消息数阈值，超过后触发压缩 |
| `tokenRatio` | Double | 0.65 | `ai.memory.token-ratio` | token 占比阈值，估算 token ≥ maxToken × tokenRatio 时触发 |
| `minCompressionTokenThreshold` | Int | 2048 | `ai.memory.min-compression-token-threshold` | 最小压缩 token 阈值，低于此值不执行压缩 |
| `lastKeep` | Int | 6 | `ai.memory.last-keep` | 压缩时保留最近 N 条消息不压缩 |
| `currentRoundCompressionRatio` | Double | 0.3 | `ai.memory.current-round-compression-ratio` | 压缩目标比例（压缩后 ≈ 原始 × 此值） |
| `largePayloadThreshold` | Long | 5120 | `ai.memory.large-payload-threshold` | 单条消息超此字节数视为"大消息"，触发 offload |
| `offloadSinglePreview` | Int | 500 | `ai.memory.offload-single-preview` | offload 后保留的预览字符数 |
| `minConsecutiveToolMessages` | Int | 6 | `ai.memory.min-consecutive-tool-messages` | 连续工具消息达此数量触发工具消息压缩 |
| `maxToken` | Long | 131072 (128K) | `ai.memory.max-token` | 模型 context window（token 数），**必须与实际模型一致** |

---

## 3. 参数分组与依赖关系

> **核心原则：9 个参数不是独立的，实际只需关注 3 个决策组。调优时按组思考，每次只动 1 个组。**

```
┌───────────────────────────────────────────────────────┐
│  组1: 何时压缩？（触发条件）                            │
│  ┌─────────────────┐  ┌────────────────┐              │
│  │ msgThreshold    │  │ tokenRatio     │  ← 满足任一   │
│  │ (消息数)         │  │ (token 占比)    │    即触发     │
│  └─────────────────┘  └────────────────┘              │
│  ┌──────────────────────────────────┐                 │
│  │ minCompressionTokenThreshold    │  ← 兜底：太少不压 │
│  └──────────────────────────────────┘                 │
├───────────────────────────────────────────────────────┤
│  组2: 怎么压缩？（压缩策略）                            │
│  ┌─────────────────┐  ┌─────────────────────────────┐ │
│  │ lastKeep        │  │ currentRoundCompressionRatio│ │
│  │ (保留几条)       │  │ (压到多少)                   │ │
│  └─────────────────┘  └─────────────────────────────┘ │
├───────────────────────────────────────────────────────┤
│  组3: 大消息怎么办？（Offload 策略）                    │
│  ┌────────────────────────┐  ┌─────────────────────┐  │
│  │ largePayloadThreshold  │  │ offloadSinglePreview│  │
│  │ (多大算"大消息")        │  │ (offload后留多少预览)│  │
│  └────────────────────────┘  └─────────────────────┘  │
│  ┌──────────────────────────────┐                     │
│  │ minConsecutiveToolMessages   │  ← 连续工具消息压缩  │
│  └──────────────────────────────┘                     │
├───────────────────────────────────────────────────────┤
│  maxToken  ← 模型硬约束，跟着模型走，一般不需要调       │
└───────────────────────────────────────────────────────┘
```

### 组内参数联动关系

#### 组1 内部联动

```
tokenRatio 的实际触发点 = maxToken × tokenRatio

例：maxToken=225280, tokenRatio=0.65 → 触发点 = 146,432 tokens

如果 maxToken 变了，触发点自动跟着变，tokenRatio 一般不用调。
```

`msgThreshold` 与 `tokenRatio` 是 **OR** 关系：
- 短对话但消息很多（如频繁工具调用）→ 靠 `msgThreshold` 触发
- 长对话但消息少（如单条回复特别长）→ 靠 `tokenRatio` 触发

#### 组2 内部联动

```
lastKeep ↑ → 被压缩的消息范围 ↓ → 压缩效果 ↓（但近期上下文保护更好）
lastKeep ↓ → 被压缩的消息范围 ↑ → 压缩效果 ↑（但可能丢失近期关键信息）

currentRoundCompressionRatio ↓ → 压得更狠 → 信息损失风险 ↑
currentRoundCompressionRatio ↑ → 压得更轻 → 可能需要更频繁压缩
```

#### 组3 内部联动

```
largePayloadThreshold ↓ → 更多消息被 offload → offloadSinglePreview 的重要性 ↑
largePayloadThreshold ↑ → 只有极大消息被 offload → offloadSinglePreview 影响面 ↓
```

---

## 4. 模型适配指南

### `maxToken` 计算公式

```
maxToken = Context Window - 最大输出 token - 安全余量

推荐：maxToken = Context Window × 0.85
```

> **注意**：`maxToken` 是 `AiMemoryProperties` 中唯一必须与模型严格匹配的参数。切换模型时必须同步更新。

### 主流模型参考

| 模型 | Context Window | Max Output | 推荐 maxToken | 说明 |
|---|---|---|---|---|
| **Kimi K2.5** | 256K (262,144) | 32K (32,768) | **225,280** (220K) | 256K - 32K - 5K 余量 |
| Kimi K2 Turbo | 256K (262,144) | 32K (32,768) | **225,280** (220K) | 同 K2.5 |
| Claude 3.5 Sonnet | 200K (200,000) | 8K (8,192) | **163,840** (160K) | 200K × 0.82 |
| GPT-4o | 128K (128,000) | 16K (16,384) | **106,496** (104K) | 128K × 0.83 |
| GPT-4 Turbo | 128K (128,000) | 4K (4,096) | **106,496** (104K) | 128K × 0.83 |
| DeepSeek V3 | 64K (65,536) | 8K (8,192) | **53,248** (52K) | 64K × 0.81 |

### maxToken 变更联动清单

当 `maxToken` 变更时，需要检查以下参数是否仍合理：

| 检查项 | 判断依据 | 动作 |
|---|---|---|
| `tokenRatio` | 新触发点 = 新 maxToken × tokenRatio，是否仍在合理范围 | 一般不用改 |
| `minCompressionTokenThreshold` | 窗口越大，此值可以越大（避免小上下文浪费压缩 LLM 调用） | 128K 建议 2048；256K 建议 4096 |
| `PAYLOAD_WARN_BYTES`（Hook 常量） | 窗口变大后告警阈值也需相应调高 | 需改代码 |

---

## 5. 数据驱动调优

### 5.1 监控基础设施

项目已内置完整的监控链路：

| 组件 | 位置 | 作用 |
|---|---|---|
| `AgentStageTimingHook` | `ai/hook/AgentStageTimingHook.kt` | 监听 agentscope Hook 事件，记录每个阶段耗时 |
| `T_AI_AGENT_STAGE` 表 | MySQL | 持久化存储所有阶段记录 |
| `CompressionEvent` | SDK 内置 | 压缩事件详情（tokenBefore/After、压缩输入输出 token） |
| 启动日志 | `AiMemoryConfig` | 打印 `[MemoryConfig] AutoContextConfig: ...` |

#### T_AI_AGENT_STAGE 表结构

| 列名 | 类型 | 说明 |
|---|---|---|
| `ID` | VARCHAR | 主键，UUID |
| `SESSION_ID` | VARCHAR | 会话 ID（线程 ID） |
| `AGENT_NAME` | VARCHAR | 智能体名称 |
| `STAGE_INDEX` | INT | 阶段序号（同一 session 内递增） |
| `STAGE_TYPE` | VARCHAR | `REASONING` / `TOOL_CALL` / `AGENT_CALL` / `CONTEXT_SUMMARY` / `ERROR` |
| `TOOL_NAME` | VARCHAR | 工具名称 / 模型名称（压缩阶段） |
| `TOOL_CALL_ID` | VARCHAR | 工具调用 ID / Agent ID |
| `STATUS` | VARCHAR | `RUNNING` / `SUCCESS` / `ERROR` |
| `DURATION_MS` | BIGINT | 耗时（毫秒），RUNNING 状态为 -1 |
| `INPUT_BRIEF` | VARCHAR(1024) | 输入摘要（payload 大小、迭代次数等） |
| `OUTPUT_BRIEF` | VARCHAR(1024) | 输出摘要（压缩详情、工具返回大小等） |
| `CREATED_TIME` | DATETIME | 创建时间 |
| `UPDATED_TIME` | DATETIME | 更新时间 |

### 5.2 诊断 SQL

#### SQL 1：压缩触发频率与效果（调优组1 和 组2）

```sql
-- 查看压缩事件：多久触发一次？压缩效果如何？
SELECT 
    SESSION_ID,
    AGENT_NAME,
    CREATED_TIME,
    DURATION_MS,
    OUTPUT_BRIEF,
    -- 解析压缩详情字段
    SUBSTRING_INDEX(SUBSTRING_INDEX(OUTPUT_BRIEF, 'tokenBefore=', -1), ',', 1) AS token_before,
    SUBSTRING_INDEX(SUBSTRING_INDEX(OUTPUT_BRIEF, 'tokenAfter=', -1), ',', 1)  AS token_after,
    SUBSTRING_INDEX(SUBSTRING_INDEX(OUTPUT_BRIEF, 'compressIn=', -1), ',', 1)  AS compress_input_token,
    SUBSTRING_INDEX(SUBSTRING_INDEX(OUTPUT_BRIEF, 'compressOut=', -1), ',', 1) AS compress_output_token
FROM T_AI_AGENT_STAGE
WHERE STAGE_TYPE = 'CONTEXT_SUMMARY'
ORDER BY CREATED_TIME DESC
LIMIT 50;
```

**信号解读表**：

| 观察到的现象 | 含义 | 要调的参数 | 方向 |
|---|---|---|---|
| `token_before` 经常接近 `maxToken` | `tokenRatio` 触发太晚，有超限风险 | `tokenRatio` | 调**低**（如 0.6 → 0.55） |
| 压缩事件几乎不出现 | 对话通常较短或阈值太高 | 可能不需要调 | — |
| `compress_input_token` 很高（>50K） | 每次压缩成本高（LLM 调用费用） | `lastKeep` | 调**高**（减少压缩范围） |
| `token_after / token_before` > `compressionRatio` | 压缩效果不达标 | `currentRoundCompressionRatio` 或 `lastKeep` | ratio 调**低**，或 lastKeep 调**低** |
| 两次压缩间隔 < 3 轮 | 压缩过于频繁，影响体验 | `tokenRatio` 或 `msgThreshold` | 调**高** |

#### SQL 2：大工具返回分布（调优组3）

```sql
-- 查看工具返回大小分布，决定 largePayloadThreshold
SELECT 
    TOOL_NAME,
    COUNT(*) AS call_count,
    AVG(
      CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(OUTPUT_BRIEF, 'chars=', -1), ',', 1) AS UNSIGNED)
    ) AS avg_chars,
    MAX(
      CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(OUTPUT_BRIEF, 'chars=', -1), ',', 1) AS UNSIGNED)
    ) AS max_chars,
    ROUND(
      AVG(CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(OUTPUT_BRIEF, 'chars=', -1), ',', 1) AS UNSIGNED))
      / 1024, 1
    ) AS avg_kb
FROM T_AI_AGENT_STAGE
WHERE STAGE_TYPE = 'TOOL_CALL' 
  AND STATUS = 'SUCCESS'
GROUP BY TOOL_NAME
ORDER BY avg_chars DESC;
```

**信号解读表**：

| 观察到的现象 | 含义 | 要调的参数 | 方向 |
|---|---|---|---|
| 有工具 `avg_kb` 在 3~5KB | 逃过 offload（默认 5120 bytes） | `largePayloadThreshold` | 调**低**（如 3072） |
| 所有工具返回 < 1KB | 当前阈值足够 | 不需要调 | — |
| offload 后模型回答质量下降 | 预览太短，关键信息丢失 | `offloadSinglePreview` | 调**高**（如 800） |

#### SQL 3：每轮对话消息数增长（调优 msgThreshold）

```sql
-- 查看每个 session 的消息数分布，对标 msgThreshold
SELECT 
    SESSION_ID,
    COUNT(CASE WHEN STAGE_TYPE = 'REASONING' THEN 1 END)       AS reasoning_count,
    COUNT(CASE WHEN STAGE_TYPE = 'TOOL_CALL' THEN 1 END)       AS tool_call_count,
    COUNT(CASE WHEN STAGE_TYPE = 'CONTEXT_SUMMARY' THEN 1 END) AS compression_count,
    -- 每次 reasoning 约产生 3 条消息（user/assistant/tool_result）
    COUNT(CASE WHEN STAGE_TYPE = 'REASONING' THEN 1 END) * 3 + 2 AS est_msg_count
FROM T_AI_AGENT_STAGE
WHERE AGENT_NAME = 'BkCI-Supervisor'
GROUP BY SESSION_ID
ORDER BY reasoning_count DESC
LIMIT 30;
```

**信号解读表**：

| 观察到的现象 | 含义 | 要调的参数 | 方向 |
|---|---|---|---|
| 大多数 session 的 `est_msg_count` < 15 | `msgThreshold=20` 合理，大部分不会触发 | 不需要调 | — |
| 大多数 > 25 | 消息积累过多才触发，太晚了 | `msgThreshold` | 调**低**（如 15） |
| 大多数 < 8 | 对话都很短，消息触发条件基本无用 | 可以保持现状 | — |

#### SQL 4：Payload 大小趋势（综合判断）

```sql
-- 查看每次 reasoning 的 payload 大小趋势
SELECT 
    SESSION_ID,
    STAGE_INDEX,
    INPUT_BRIEF,
    SUBSTRING_INDEX(SUBSTRING_INDEX(INPUT_BRIEF, 'totalKB=', -1), ',', 1) AS total_kb,
    SUBSTRING_INDEX(SUBSTRING_INDEX(INPUT_BRIEF, 'msgs=', -1), ',', 1)    AS msg_count
FROM T_AI_AGENT_STAGE
WHERE STAGE_TYPE = 'REASONING'
  AND AGENT_NAME = 'BkCI-Supervisor'
ORDER BY SESSION_ID, STAGE_INDEX
LIMIT 100;
```

---

### 5.3 日志快速排查

#### Cheat Sheet

```bash
# 1. 看启动时的实际配置值
grep "\[MemoryConfig\] AutoContextConfig" app.log

# 2. 看压缩是否触发、效果如何
grep "\[Timing\] ContextSummary END" app.log | tail -20

# 3. 看是否有超大 payload（接近 context limit）
grep "\[Timing\] LARGE payload" app.log | tail -10

# 4. 看是否有超大工具返回（应该被 offload）
grep "\[Timing\] LARGE tool result" app.log | tail -10

# 5. 看工具执行失败
grep "\[Timing\] Tool FAILED" app.log | tail -10

# 6. 看慢工具调用（>3s）
grep "\[Timing\] SLOW tool" app.log | tail -10
```

#### 日志关键字段对照

| 日志关键字 | 对应 Hook 事件 | 包含信息 |
|---|---|---|
| `[Timing] Reasoning START` | `PreReasoningEvent` | `totalKB`, `msgsKB`, `toolsKB`, `msgs` |
| `[Timing] Reasoning END` | `PostReasoningEvent` | `duration`, `toolCalls` |
| `[Timing] ContextSummary START` | `PreSummaryEvent` | `iter=当前/最大` |
| `[Timing] ContextSummary END` | `PostSummaryEvent` | `summaryLen`, `tokenBefore/After`, `compressIn/Out` |
| `[Timing] LARGE payload` | `PreReasoningEvent` | 超过 512KB 告警 |
| `[Timing] LARGE tool result` | `PostActingEvent` | 超过 50K 字符告警 |
| `[Timing] SLOW tool` | `PostActingEvent` | 超过 3s 告警 |
| `[Timing] Tool FAILED` | `PostActingEvent` | 工具入参 + 错误返回 |

---

## 6. 调优 SOP（标准作业流程）

```
┌──────────────────────────────────────────────────┐
│  Step 1: 收集基线数据                              │
│                                                    │
│  ① 确认当前配置：grep [MemoryConfig] 启动日志       │
│  ② 跑 SQL 1~4，记录关键百分位数：                   │
│     - P50/P90/P99 的 token 使用量                  │
│     - 压缩触发频率（每 N 个 session 触发几次）       │
│     - 工具返回大小分布                              │
│  ③ 记录当前异常现象（如 413 错误、回答质量下降等）   │
└────────────────────┬─────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────┐
│  Step 2: 定位问题组                                │
│                                                    │
│  问题现象               → 对应组                    │
│  ─────────────────────────────────────             │
│  413 错误 / 超限         → 组1（触发太晚）           │
│  压缩过于频繁            → 组1（触发太早）           │
│  压缩后回答质量下降       → 组2（lastKeep 太小）      │
│  压缩 LLM 成本太高       → 组2（压缩范围太大）       │
│  大量 token 被工具返回占用 → 组3（offload 不够积极）  │
│  offload 后模型丢关键信息 → 组3（preview 太短）      │
└────────────────────┬─────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────┐
│  Step 3: 改参数（每次只改 1 个组）                  │
│                                                    │
│  通过 application.yml 覆盖，不改代码：              │
│                                                    │
│  ai:                                               │
│    memory:                                         │
│      token-ratio: 0.55    # 示例：调低触发阈值      │
│                                                    │
│  重启服务，确认启动日志中新值已生效                   │
└────────────────────┬─────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────┐
│  Step 4: 灰度验证                                  │
│                                                    │
│  在测试环境跑 3 类典型场景：                         │
│                                                    │
│  ① 短对话（1-2 轮）→ 不应触发压缩                   │
│  ② 长对话（8-10 轮）→ 必须触发压缩                  │
│  ③ 大工具返回（如模板查询）→ 应触发 offload          │
│                                                    │
│  关注：回答质量是否下降？压缩是否在合理时机触发？     │
└────────────────────┬─────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────┐
│  Step 5: 再次跑 SQL 对比                           │
│                                                    │
│  对比 Step 1 的基线：                               │
│  ✓ 压缩次数是否合理？                              │
│  ✓ 413 错误是否消失？                              │
│  ✓ compress_input_token 是否下降？（省钱）          │
│  ✓ 回答质量是否可接受？                            │
│                                                    │
│  满意 → 上线；不满意 → 回到 Step 2                  │
└──────────────────────────────────────────────────┘
```

---

## 7. 典型场景与推荐配置

### 场景 A：Kimi K2.5（256K 窗口）— 通用 Agent

```yaml
ai:
  memory:
    max-token: 225280                          # 220K = 256K - 32K(output) - 4K(余量)
    msg-threshold: 20
    token-ratio: 0.65                          # 触发点 ≈ 146K tokens
    min-compression-token-threshold: 4096      # 窗口大，小上下文不值得压缩
    last-keep: 6
    current-round-compression-ratio: 0.3
    large-payload-threshold: 5120
    offload-single-preview: 500
    min-consecutive-tool-messages: 6
```

### 场景 B：GPT-4o（128K 窗口）— 通用 Agent

```yaml
ai:
  memory:
    max-token: 106496                          # 104K = 128K - 16K(output) - 8K(余量)
    msg-threshold: 20
    token-ratio: 0.65                          # 触发点 ≈ 69K tokens
    min-compression-token-threshold: 2048
    last-keep: 6
    current-round-compression-ratio: 0.3
    large-payload-threshold: 5120
    offload-single-preview: 500
    min-consecutive-tool-messages: 6
```

### 场景 C：DeepSeek V3（64K 窗口）— 窗口较小，需积极压缩

```yaml
ai:
  memory:
    max-token: 53248                           # 52K = 64K - 8K(output) - 4K(余量)
    msg-threshold: 15                          # 窗口小，更早触发
    token-ratio: 0.55                          # 更激进的 token 触发
    min-compression-token-threshold: 1024      # 窗口小，低阈值也要压缩
    last-keep: 4                               # 少保留，给更多空间
    current-round-compression-ratio: 0.25      # 压得更狠
    large-payload-threshold: 3072              # 更积极的 offload
    offload-single-preview: 300                # 预览也缩短
    min-consecutive-tool-messages: 4           # 更早压缩连续工具消息
```

### 场景 D：高频工具调用场景（子智能体 / BuildArtifact 等）

```yaml
ai:
  memory:
    max-token: 225280
    msg-threshold: 15                          # 工具调用密集，消息积累快
    token-ratio: 0.6                           # 稍早触发
    min-compression-token-threshold: 4096
    last-keep: 4                               # 工具消息价值衰减快，少保留
    current-round-compression-ratio: 0.25      # 压得更狠
    large-payload-threshold: 3072              # 工具返回容易偏大
    offload-single-preview: 800                # 但预览要多留，防止丢关键信息
    min-consecutive-tool-messages: 4           # 连续工具消息更常见
```

---

## 8. 配置参考

### 完整 YAML 模板（带注释）

```yaml
ai:
  memory:
    # ═══════════════════════════════════════════════════
    # 模型硬约束（必须与实际模型一致）
    # ═══════════════════════════════════════════════════
    # 计算公式：Context Window - Max Output - 安全余量
    # Kimi K2.5: 256K - 32K - 4K = 220K = 225280
    max-token: 225280

    # ═══════════════════════════════════════════════════
    # 组1: 压缩触发条件（满足任一即触发）
    # ═══════════════════════════════════════════════════
    # 消息数阈值
    msg-threshold: 20
    # token 占比阈值（实际触发点 = maxToken × tokenRatio）
    token-ratio: 0.65
    # 最小压缩 token 阈值（低于此值不执行压缩，兜底保护）
    min-compression-token-threshold: 4096

    # ═══════════════════════════════════════════════════
    # 组2: 压缩策略
    # ═══════════════════════════════════════════════════
    # 压缩时保留最近 N 条消息不压缩
    last-keep: 6
    # 压缩目标比例（压缩后大小 = 原始 × 此值）
    current-round-compression-ratio: 0.3

    # ═══════════════════════════════════════════════════
    # 组3: 大消息 Offload 策略
    # ═══════════════════════════════════════════════════
    # 单条消息超此字节数触发 offload
    large-payload-threshold: 5120
    # offload 后保留的预览字符数
    offload-single-preview: 500
    # 连续工具消息达此数量触发压缩
    min-consecutive-tool-messages: 6
```

### Spring Boot relaxed binding 支持

以下写法等价，Spring Boot 均可识别：

```yaml
# kebab-case（推荐）
msg-threshold: 20

# camelCase
msgThreshold: 20

# snake_case
msg_threshold: 20

# UPPER_CASE（环境变量）
AI_MEMORY_MSG_THRESHOLD=20
```

---

## 9. FAQ

### Q1: 改了 YAML 配置后需要重启吗？

**需要**。当前 `AutoContextConfig` 是启动时由 `AiMemoryConfig` 构建的 `@Bean`，改 YAML 后需要重启才生效。
未来如需热更新，可以将 `AiMemoryConfig` 改为 `@RefreshScope`。

### Q2: 如何确认配置是否生效？

查看启动日志：

```bash
grep "\[MemoryConfig\] AutoContextConfig" app.log
```

会打印所有 9 个参数的当前值。

### Q3: 压缩用的 LLM 调用会产生额外费用吗？

**会**。每次压缩会调用 LLM 进行摘要，产生 `compressIn`（输入 token）+ `compressOut`（输出 token）的费用。
通过 SQL 1 可以监控这部分成本。优化方向：

- 调高 `lastKeep` → 减少压缩输入 token（更少消息需要被摘要）
- 适当调高 `tokenRatio` → 减少压缩触发频率（但要平衡超限风险）

### Q4: msgThreshold 和 tokenRatio 哪个更重要？

取决于你的场景：
- **工具调用密集型**（如 Supervisor 调度子智能体）：`msgThreshold` 更重要，因为消息数增长快但每条不大
- **长文本型**（如代码分析）：`tokenRatio` 更重要，因为单条消息可能就很大

### Q5: 子智能体需要单独配置吗？

当前所有智能体共用同一个 `AutoContextConfig` Bean。如果子智能体的特征与主智能体差异很大（如高频短对话 vs 低频长对话），建议未来通过 `@Qualifier` 区分不同配置。

### Q6: 怎么判断 offload 是否生效了？

观察以下指标：
1. `[Timing] LARGE tool result` 日志是否出现（单工具返回 > 50K 字符）
2. SQL 2 中各工具的 `avg_kb`，与 `largePayloadThreshold / 1024` 对比
3. 压缩事件中 `tokenBefore` 是否因 offload 而降低

### Q7: 三条核心原则是什么？

1. **按组调，不按单参数调** — 9 个参数只有 3 个决策组
2. **数据驱动，不拍脑袋** — 跑 SQL 看基线，改参数后对比
3. **每次只动 1 个组** — 控制变量，才知道哪个改动有效
