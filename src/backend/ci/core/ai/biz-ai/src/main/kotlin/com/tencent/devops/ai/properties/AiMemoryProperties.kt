package com.tencent.devops.ai.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 智能体记忆相关配置属性，对应 AutoContextMemory 的压缩策略参数。
 *
 * ## 单位说明
 *
 * SDK 内部有两种度量单位，使用时容易混淆：
 *
 * - **token 数**：LLM 的计费/计量单位。SDK 用 `字符数 / 2.5` 估算，
 *   Kimi 实际约 `字符数 / 3.5`（1 token ≈ 1.5-2 个汉字）。
 * - **字符数**：`String.length()` 返回值。1 个汉字 = 1 个字符，
 *   1 个英文字母 = 1 个字符，与字节数近似（常见字符 ≈ 1 字节）。
 *
 * 各参数单位：
 * - [maxToken]、[minCompressionTokenThreshold] → token 数
 * - [largePayloadThreshold]、[offloadSinglePreview] → 字符数
 * - [msgThreshold]、[lastKeep]、[minConsecutiveToolMessages] → 消息条数
 * - [tokenRatio]、[currentRoundCompressionRatio] → 比例 (0.0-1.0)
 *
 * ## 换算参考（基于 SDK 估算 chars/2.5）
 *
 * | token 数 | ≈ 字符数 | ≈ 中文字数 |
 * |----------|----------|-----------|
 * | 1K       | 2,500    | 2,500     |
 * | 10K      | 25,000   | 2.5 万    |
 * | 100K     | 250,000  | 25 万     |
 *
 * ## 压缩触发条件（满足任一）
 *
 * - 消息数 ≥ [msgThreshold]
 * - 估算 token 数 ≥ [maxToken] × [tokenRatio]
 */
@ConfigurationProperties("ai.memory")
data class AiMemoryProperties(
    /**
     * 模型 context window 大小。
     * 单位：token 数。
     * Kimi K2.5 = 262,144 tokens，减去 ~36K 输出预留 ≈ 225,280。
     * 换算：225,280 tokens ≈ 563K 字符 ≈ 56 万字中文。
     */
    val maxToken: Long = 225_280,

    /**
     * 消息数阈值，对话消息数 ≥ 此值时触发压缩。
     * 单位：消息条数。
     */
    val msgThreshold: Int = 100,

    /**
     * token 占比阈值，估算 token 达到 maxToken × tokenRatio 时触发压缩。
     * 单位：比例 (0.0-1.0)。
     * 当前值 0.7 → 触发点 = 225,280 × 0.7 ≈ 157K tokens ≈ 394K 字符。
     */
    val tokenRatio: Double = 0.7,

    /**
     * 最小压缩 token 阈值，低于此值即使满足触发条件也不执行压缩。
     * 单位：token 数。
     * 4,096 tokens ≈ 10K 字符 ≈ 1 万字中文。
     */
    val minCompressionTokenThreshold: Int = 4096,

    /**
     * 压缩时保留最近 N 条消息不压缩（保护近期上下文完整性）。
     * 单位：消息条数。
     * 仅在 Strategy 1（工具调用压缩）和 Strategy 2（大消息 offload）中生效。
     */
    val lastKeep: Int = 20,

    /**
     * 当前轮次压缩比例，压缩后目标大小 = 原始 × 此值。
     * 单位：比例 (0.0-1.0)。
     * 0.4 表示压缩后保留原始内容的 30%。
     */
    val currentRoundCompressionRatio: Double = 0.3,

    /**
     * 单条消息超过此值视为"大消息"，触发 offload（不调 LLM，零额外成本）。
     * 单位：字符数（String.length()）。
     * 100,000 字符 ≈ 40K tokens ≈ 10 万字中文 ≈ 100KB 文本。
     */
    val largePayloadThreshold: Long = 100_000,

    /**
     * 大消息被 offload 后保留的预览长度。
     * 单位：字符数（String.length()）。
     * 10,000 字符 ≈ 4K tokens ≈ 1 万字中文。
     */
    val offloadSinglePreview: Int = 10_000,

    /**
     * 连续工具消息达到此数量时触发工具消息压缩（Strategy 1）。
     * 单位：消息条数。
     */
    val minConsecutiveToolMessages: Int = 8
)
