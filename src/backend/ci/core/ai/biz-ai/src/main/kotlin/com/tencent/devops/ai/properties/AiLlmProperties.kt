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
     * 设为 60s，兼顾大模型"思考"耗时（deepseek-r1 等推理模型首 token 可能 20-40s）。
     * 此值应 ≥ executionTimeoutSeconds，作为底层兜底。
     */
    val readTimeoutSeconds: Long = 60,
    /** 请求写入超时（秒），prompt 较大时需要一定上传时间 */
    val writeTimeoutSeconds: Long = 30,

    // ---- 模型调用重试配置（参考 OpenAI SDK / 业界最佳实践） ----
    /**
     * 单次模型调用超时（秒）。
     * agentscope 框架在 Flux 流上施加 timeout()，即从发起请求到首元素的等待上限。
     * OpenAI SDK 默认 600s 过于宽松；业界推荐流式场景 30-60s。
     * 默认 30s：大多数模型 5-30s 内开始返回首 token，30s 留足余量。
     */
    val executionTimeoutSeconds: Long = 30,
    /**
     * 最大尝试次数（含首次调用）。
     * OpenAI SDK 默认 3 次（首次 + 2 次重试），业界共识也是 3 次。
     */
    val maxAttempts: Int = 3,
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
