package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "CDS 活跃工作空间（含数字分身 Agent）")
data class CdsActiveWorkspace(
    @get:Schema(title = "CDS 记录 ID")
    val id: Long? = null,
    @get:Schema(title = "工作空间名称")
    val workspaceName: String,
    @get:Schema(title = "Agent ID")
    val agentId: String? = null,
    @get:Schema(title = "展示名")
    val displayName: String? = null,
    @get:Schema(title = "环境 ID")
    val envId: String? = null,
    @get:Schema(title = "主机 IP")
    val hostIp: String? = null,
    @get:Schema(title = "所属者")
    val owner: String? = null,
    @get:Schema(title = "状态")
    val status: String? = null
)
