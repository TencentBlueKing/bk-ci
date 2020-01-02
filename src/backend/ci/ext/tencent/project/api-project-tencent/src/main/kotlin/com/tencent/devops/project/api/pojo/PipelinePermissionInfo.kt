package com.tencent.devops.project.api.pojo

import io.swagger.annotations.ApiModelProperty

data class PipelinePermissionInfo(
    @ApiModelProperty("目标用Id")
    val userId: String,
    @ApiModelProperty("目标用Id列表")
    val userIds: List<String>?,
    @ApiModelProperty("项目Code")
    val projectId: String,
    @ApiModelProperty("权限名称")
    val permission: String,
    @ApiModelProperty("资源类型")
    val resourceType: String,
    @ApiModelProperty("资源Code,如pipelineId,projectId")
    val resourceTypeCode: String
)