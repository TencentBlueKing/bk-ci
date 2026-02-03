package com.tencent.devops.process.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 云桌面基础信息
 */
data class WorkspaceBaseInfo(
    @get:Schema(title = "工作空间名称", name = "workspace_name")
    @JsonProperty("workspace_name")
    val workspaceName: String,
    @get:Schema(title = "项目ID", name = "project_id")
    @JsonProperty("project_id")
    val projectId: String,
    @get:Schema(title = "inner_ip", name = "inner_ip")
    @JsonProperty("inner_ip")
    val innerIp: String?,
    @get:Schema(title = "云桌面别名", name = "display_name")
    @JsonProperty("display_name")
    val displayName: String? = null
)
