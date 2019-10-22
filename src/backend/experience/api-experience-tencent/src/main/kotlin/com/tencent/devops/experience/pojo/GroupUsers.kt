package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-体验组用户")
data class GroupUsers(
    @ApiModelProperty("内部人员")
    val innerUsers: Set<String>,
    @ApiModelProperty("外部人员")
    val outerUsers: Set<String>
)