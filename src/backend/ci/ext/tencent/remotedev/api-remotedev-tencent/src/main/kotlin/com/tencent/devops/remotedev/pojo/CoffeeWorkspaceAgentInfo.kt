package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "CDS AI云桌面工作空间（仅解析agent类型所需字段）")
data class CoffeeWorkspaceAgentInfo(
    @get:Schema(title = "工作空间名称")
    val workspaceName: String,
    @get:Schema(title = "AI分身agent类型，如OPENCLAW")
    val agentMode: String? = null
)
