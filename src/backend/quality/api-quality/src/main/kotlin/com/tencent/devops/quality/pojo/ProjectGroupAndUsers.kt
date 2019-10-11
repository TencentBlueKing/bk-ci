package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-项目用户组和所有人员")
data class ProjectGroupAndUsers(
    @ApiModelProperty("组名称")
    val groupName: String,
    @ApiModelProperty("组ID")
    val groupId: String,
    @ApiModelProperty("组员")
    val users: Set<String>
)