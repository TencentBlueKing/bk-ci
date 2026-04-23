package com.tencent.devops.ai.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/** LLM 模型连接配置属性（API URL、API Key、模型名称、HTTP 超时等）。 */
@ConfigurationProperties("ai.llm")
data class AiLlmProperties(
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "deepseek-chat",
    /** 蓝鲸 API 网关 bk_app_code，非空时启用网关认证模式 */
    val bkAppCode: String = "",
    /** 蓝鲸 API 网关 bk_app_secret */
    val bkAppSecret: String = "",
    /** TCP 连接超时（秒） */
    val connectTimeoutSeconds: Long = 10,
    /**
     * HTTP 读超时（秒），即底层 socket 两次数据包之间的最大等待时间。
     * 流式场景下：首 token 到达前、以及任意两个 SSE chunk 之间都受此约束。
     * 此值必须 > executionTimeoutSeconds，确保 Reactor 层的 timeout 先触发
     * （优雅取消 + 重试），而不是 socket 层先断开（粗暴 IOException）。
     */
    val readTimeoutSeconds: Long = 90,
    /** 请求写入超时（秒），prompt 较大时需要一定上传时间 */
    val writeTimeoutSeconds: Long = 30,

    // ---- 模型调用重试配置（参考 OpenAI SDK / 业界最佳实践） ----
    /**
     * 单次模型调用超时（秒）。
     * agentscope 框架在 Flux 流上施加 timeout()，即从发起请求到首元素的等待上限。
     * 默认 60s：推理模型（deepseek-r1 等）首 token 可达 20-40s，
     * 30s 过于紧凑导致频繁误判超时；60s 兼顾常规模型和推理模型。
     */
    val executionTimeoutSeconds: Long = 60,
    /**
     * 最大尝试次数（含首次调用）。
     * 设为 5 次（首次 + 4 次重试）。比 OpenAI SDK 默认的 3 次更多，
     * 因为 Reactor timeout 取消上游订阅时会产生一个级联的 SSE stream failed
     * 副作用错误，该错误也会消耗一次重试次数（详见日志分析），
     * 实际有效重试次数约为 maxAttempts/2。
     */
    val maxAttempts: Int = 5,
    /**
     * 首次重试退避间隔（秒）。
     * OpenAI SDK 用 0.5s，业界推荐 0.5-2s。
     * 设为 1s：避免立即重试加重后端压力，但又不会让用户多等。
     */
    val initialBackoffSeconds: Long = 1,
    /**
     * 最大重试退避间隔（秒）。
     * OpenAI SDK 上限 8s，业界推荐 8-30s。
     * 设为 8s：3 次尝试退避序列为 1s → 2s（实际上限 8s），不会过度等待。
     */
    val maxBackoffSeconds: Long = 8,
    /** 退避倍数，默认 2.0（指数退避），与 OpenAI SDK 一致 */
    val backoffMultiplier: Double = 2.0
)
