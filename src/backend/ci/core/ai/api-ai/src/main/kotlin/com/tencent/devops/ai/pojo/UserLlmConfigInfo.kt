package com.tencent.devops.ai.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户自定义大模型-配置详情")
data class UserLlmConfigInfo(
    @get:Schema(title = "归属用户")
    val userId: String,
    @get:Schema(title = "模型服务地址")
    val baseUrl: String,
    @get:Schema(title = "模型名称")
    val modelName: String,
    @get:Schema(title = "是否已配置 API Key")
    val hasApiKey: Boolean,
    @get:Schema(title = "蓝鲸网关模式 bk_app_code")
    val bkAppCode: String,
    @get:Schema(title = "是否已配置 bk_app_secret")
    val hasBkAppSecret: Boolean,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "TCP 连接超时（秒）")
    val connectTimeoutSeconds: Long,
    @get:Schema(title = "HTTP 读超时（秒）")
    val readTimeoutSeconds: Long,
    @get:Schema(title = "请求写入超时（秒）")
    val writeTimeoutSeconds: Long,
    @get:Schema(title = "单次模型调用超时（秒）")
    val executionTimeoutSeconds: Long,
    @get:Schema(title = "最大尝试次数（含首次调用）")
    val maxAttempts: Int,
    @get:Schema(title = "首次重试退避间隔（秒）")
    val initialBackoffSeconds: Long,
    @get:Schema(title = "最大重试退避间隔（秒）")
    val maxBackoffSeconds: Long,
    @get:Schema(title = "退避倍数")
    val backoffMultiplier: Double,
    @get:Schema(title = "创建时间")
    val createdTime: Long,
    @get:Schema(title = "更新时间")
    val updatedTime: Long
)
