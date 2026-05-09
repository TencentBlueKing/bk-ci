package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "开启AI功能的云桌面信息")
data class WorkspaceAiInfo(
    @get:Schema(title = "云桌面Name", required = true)
    val workspaceName: String,
    @get:Schema(title = "云桌面别名（展示名称）", required = true)
    val displayName: String,
    @get:Schema(title = "云桌面IP", required = false)
    val ip: String,
    @get:Schema(title = "云桌面环境ID（envId）", required = false)
    val envId: String,
    @get:Schema(title = "项目ID", required = false)
    val projectId: String,
    @get:Schema(title = "云桌面地域类型", required = false)
    val zoneConfigType: String
)
