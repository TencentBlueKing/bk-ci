package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-体验组权限")
data class GroupPermission(
    @ApiModelProperty("是否可编辑", required = true)
    val canEdit: Boolean,
    @ApiModelProperty("是否可删除", required = true)
    val canDelete: Boolean
)