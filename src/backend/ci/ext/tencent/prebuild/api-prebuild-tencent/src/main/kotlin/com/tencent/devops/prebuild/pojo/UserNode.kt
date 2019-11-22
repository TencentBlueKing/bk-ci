package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户PreBuild节点")
data class UserNode(
    @ApiModelProperty("IP")
    val ip: String,
    @ApiModelProperty("密码")
    val pwd: String
)
