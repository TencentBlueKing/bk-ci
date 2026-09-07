package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享校验规则")
data class CreativeFlowShareValidateRules(
    @get:Schema(title = "源环境OS类型，如 LINUX/WINDOWS/MACOS")
    val envOsType: String? = null
)
