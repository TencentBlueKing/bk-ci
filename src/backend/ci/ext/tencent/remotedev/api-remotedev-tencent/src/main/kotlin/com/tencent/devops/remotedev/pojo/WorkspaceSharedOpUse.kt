package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "工作空间共享信息")
data class WorkspaceSharedOpUse(
    @Schema(description = "工作空间名称")
    val workspaceName: String,
    @Schema(description = "操作人")
    val operator: String,
    @Schema(description = "共享用户")
    val sharedUser: String,
    @Schema(description = "分配类型")
    val type: WorkspaceShared.AssignType
)
