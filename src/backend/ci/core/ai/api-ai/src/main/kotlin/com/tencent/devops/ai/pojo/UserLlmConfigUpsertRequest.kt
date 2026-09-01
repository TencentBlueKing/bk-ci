package com.tencent.devops.ai.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户自定义大模型-保存请求")
data class UserLlmConfigUpsertRequest(
    @get:Schema(title = "模型服务地址", required = true)
    val baseUrl: String,
    @get:Schema(title = "模型名称", required = true, example = "deepseek-chat")
    val modelName: String,
    @get:Schema(title = "OpenAI兼容模式 API Key，传空字符串可清空", required = false)
    val apiKey: String? = null,
    @get:Schema(title = "蓝鲸网关模式 bk_app_code，传空字符串可清空", required = false)
    val bkAppCode: String = "",
    @get:Schema(title = "蓝鲸网关模式 bk_app_secret，传空字符串可清空", required = false)
    val bkAppSecret: String? = null,
    @get:Schema(title = "是否启用", required = false)
    val enabled: Boolean = true,
    @get:Schema(title = "TCP 连接超时（秒）", required = false)
    val connectTimeoutSeconds: Long = 10,
    @get:Schema(title = "HTTP 读超时（秒）", required = false)
    val readTimeoutSeconds: Long = 90,
    @get:Schema(title = "请求写入超时（秒）", required = false)
    val writeTimeoutSeconds: Long = 30,
    @get:Schema(title = "单次模型调用超时（秒）", required = false)
    val executionTimeoutSeconds: Long = 60,
    @get:Schema(title = "最大尝试次数（含首次调用）", required = false)
    val maxAttempts: Int = 5,
    @get:Schema(title = "首次重试退避间隔（秒）", required = false)
    val initialBackoffSeconds: Long = 1,
    @get:Schema(title = "最大重试退避间隔（秒）", required = false)
    val maxBackoffSeconds: Long = 8,
    @get:Schema(title = "退避倍数", required = false)
    val backoffMultiplier: Double = 2.0
)
