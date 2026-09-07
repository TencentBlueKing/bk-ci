package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "CDS 已登记数字分身")
data class ImateAvatar(
    @get:Schema(title = "数字分身 ID，空则回退 agentId")
    val digitalAvatarId: String,
    @get:Schema(title = "Agent ID")
    val agentId: String,
    @get:Schema(title = "Agent 名称")
    val agentName: String? = null,
    @get:Schema(title = "入驻云桌面 workspaceName")
    val workspaceName: String,
    @get:Schema(title = "分身状态")
    val status: String? = null,
    @get:Schema(title = "展示名")
    val displayName: String? = null,
    @get:Schema(title = "登记所属者")
    val owner: String? = null,
    @get:Schema(title = "头像 URL，供前端直接渲染")
    val avatarUrl: String? = null
)
