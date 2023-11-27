package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安全水印")
data class SecOpsWaterMarkDTO(
    @ApiModelProperty("场景token")
    val token: String,
    @ApiModelProperty("用户名称")
    val username: String
)
