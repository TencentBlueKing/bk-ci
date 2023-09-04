package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工作空间共享信息")
data class WorkspaceSharedOpUse(
    @ApiModelProperty("工作空间名称")
    val workspaceName: String,
    @ApiModelProperty("操作人")
    val operator: String,
    @ApiModelProperty("共享用户")
    val sharedUser: String,
    @ApiModelProperty("分配类型")
    val type: WorkspaceShared.AssignType
)
