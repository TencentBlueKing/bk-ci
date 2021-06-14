package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
data class BkUserInfo(
    @ApiModelProperty("用户Id")
    val id: Int,
    @ApiModelProperty("用户名")
    val username: String,
    @ApiModelProperty("是否启用")
    val enabled: Boolean
)
