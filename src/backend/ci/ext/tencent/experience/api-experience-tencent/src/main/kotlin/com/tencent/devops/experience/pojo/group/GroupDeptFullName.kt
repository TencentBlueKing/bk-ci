package com.tencent.devops.experience.pojo.group

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("体验组--组织架构")
data class GroupDeptFullName(
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("组织架构")
    val deptFullName: String
)
