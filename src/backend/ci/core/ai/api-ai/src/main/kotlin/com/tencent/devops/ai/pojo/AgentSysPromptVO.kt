package com.tencent.devops.ai.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "智能体系统提示词")
data class AgentSysPromptVO(
    @get:Schema(title = "智能体名称")
    val agentName: String,
    @get:Schema(title = "提示词模板")
    val promptTemplate: String,
    @get:Schema(title = "说明")
    val description: String? = null,
    @get:Schema(title = "是否启用")
    val enabled: Boolean = true
)
