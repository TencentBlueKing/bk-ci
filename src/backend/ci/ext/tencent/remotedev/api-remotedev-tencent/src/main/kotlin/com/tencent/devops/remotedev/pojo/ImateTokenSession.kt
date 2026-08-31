package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建态 imate / CDS Agent 会话凭据")
data class ImateTokenSession(
    @get:Schema(title = "CDS Agent JWT")
    val token: String,
    @get:Schema(title = "JWT 有效分钟数")
    val expirationMinutes: Long,
    @get:Schema(title = "过期时间（epoch millis）")
    val expiresAt: Long,
    @get:Schema(title = "换票使用的身份用户")
    val username: String,
    @get:Schema(title = "分身 workspaceName")
    val workspaceName: String,
    @get:Schema(title = "展示名")
    val displayName: String? = null,
    @get:Schema(title = "分身实际所属者")
    val owner: String? = null,
    @get:Schema(title = "数字分身 ID")
    val digitalAvatarId: String? = null,
    @get:Schema(title = "Agent ID")
    val agentId: String? = null,
    @get:Schema(title = "Agent 名称")
    val agentName: String? = null
)
