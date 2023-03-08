package com.tencent.devops.experience.pojo.outer

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("外部登录-入参")
data class OuterLoginParam(
    @ApiModelProperty("用户名")
    val username: String,
    @ApiModelProperty("密码")
    val password: String
)
