package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("资源组详情")
data class ResourceGroupInfo(
    @ApiModelProperty("用户组ID")
    val groupId: String,
    @ApiModelProperty("用户组名称")
    val groupName: String,
    @ApiModelProperty("项目code")
    val projectCode: String,
    @ApiModelProperty("资源类型")
    val resourceType: String,
    @ApiModelProperty("资源名称")
    val resourceName: String,
    @ApiModelProperty("资源code")
    val resourceCode: String
)
