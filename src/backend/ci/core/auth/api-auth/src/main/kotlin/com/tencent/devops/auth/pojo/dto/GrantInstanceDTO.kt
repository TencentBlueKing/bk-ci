package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("实例授权入参")
data class GrantInstanceDTO(
    @ApiModelProperty("资源类型, 如：pipeline,project等")
    val resourceType: String,
    @ApiModelProperty("资源实例编码, 如：pipelineId,projectId等")
    val resourceCode: String,
    @ApiModelProperty("资源名称")
    val resourceName: String?,
    @ApiModelProperty("权限类型, 如：create,edit等")
    val permission: String,
    @ApiModelProperty("目标操作用户")
    val createUser: String
)
