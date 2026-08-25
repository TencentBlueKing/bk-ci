package com.tencent.devops.scm.pojo.tapd

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * TAPD 项目详情外层包装
 */
data class WorkspaceResponse(
    @JsonProperty("Workspace")
    val workspace: TapdWorkspace
)
