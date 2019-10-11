package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-用户组权限")
data class GroupPermission(
    @ApiModelProperty("是否可编辑", required = true)
    val canEdit: Boolean,
    @ApiModelProperty("是否可删除", required = true)
    val canDelete: Boolean
)