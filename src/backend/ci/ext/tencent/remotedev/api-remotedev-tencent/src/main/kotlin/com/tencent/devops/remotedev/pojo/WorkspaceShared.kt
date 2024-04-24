package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工作空间共享信息")
data class WorkspaceShared(
    @get:Schema(title = "Id")
    val id: Long?,
    @get:Schema(title = "工作空间名称")
    val workspaceName: String,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "共享用户")
    val sharedUser: String,
    @get:Schema(title = "分配类型")
    val type: AssignType,
    @get:Schema(title = "start resourceId")
    val resourceId: String
) {
    enum class AssignType {
        OWNER,
        VIEWER
    }
}
