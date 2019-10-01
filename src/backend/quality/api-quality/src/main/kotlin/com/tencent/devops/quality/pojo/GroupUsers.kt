package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-用户组用户")
data class GroupUsers(
    @ApiModelProperty("内部人员")
    val innerUsers: Set<String>,
    @ApiModelProperty("外部人员")
    val outerUsers: Set<String>
)