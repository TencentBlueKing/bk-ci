package com.tencent.devops.experience.pojo.outer

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("外部登录用户信息")
data class OuterProfileVO(
    @ApiModelProperty("用户名")
    val username: String,
    @ApiModelProperty("头像")
    val logo: String,
    @ApiModelProperty("邮箱")
    val email: String,
    @ApiModelProperty("类型,1--蓝鲸外部用户,2--太湖账户")
    val type: Int = 1
)
