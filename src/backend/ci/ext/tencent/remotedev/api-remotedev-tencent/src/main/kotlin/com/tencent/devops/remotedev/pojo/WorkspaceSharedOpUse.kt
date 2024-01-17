package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工作空间共享信息")
data class WorkspaceSharedOpUse(
    @Schema(title = "工作空间名称")
    val workspaceName: String,
    @Schema(title = "操作人")
    val operator: String,
    @Schema(title = "共享用户")
    val sharedUser: String,
    @Schema(title = "分配类型")
    val type: WorkspaceShared.AssignType
)
