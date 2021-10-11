package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("默认组信息")
data class DefaultGroup(
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("展示名称")
    val displayName: String,
    @ApiModelProperty("组编码")
    val code: String
)
