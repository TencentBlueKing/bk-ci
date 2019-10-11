package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-用户组信息")
data class GroupCreate(
    @ApiModelProperty("用户组名称", required = true)
    val name: String,
    @ApiModelProperty("内部人员")
    val innerUsers: Set<String>,
    @ApiModelProperty("外部人员")
    val outerUsers: String,
    @ApiModelProperty("描述")
    val remark: String?
)